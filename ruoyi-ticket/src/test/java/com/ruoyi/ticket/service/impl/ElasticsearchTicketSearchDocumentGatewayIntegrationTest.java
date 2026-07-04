package com.ruoyi.ticket.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.model.TicketAccessScope;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Elasticsearch 工单索引网关集成测试。
 */
@Testcontainers
@DisplayName("Elasticsearch 工单索引网关集成测试")
class ElasticsearchTicketSearchDocumentGatewayIntegrationTest {

    private static final String INDEX_NAME = "ticket-search-test";

    @Container
    private static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.18.8"))
            .withEnv("xpack.security.enabled", "false");

    private static RestClientTransport transport;
    private static ElasticsearchClient client;
    private static ElasticsearchTicketSearchDocumentGateway gateway;
    private static ElasticsearchTicketSearchQueryGateway queryGateway;
    private static ElasticsearchTicketSearchIndexAdminGateway indexAdminGateway;

    @BeforeAll
    static void setUpClient() {
        RestClient restClient = RestClient.builder(HttpHost.create("http://" + ELASTICSEARCH.getHttpHostAddress()))
                .build();
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
        TicketSearchProperties properties = new TicketSearchProperties();
        properties.setIndexAlias(INDEX_NAME);
        gateway = new ElasticsearchTicketSearchDocumentGateway();
        ReflectionTestUtils.setField(gateway, "elasticsearchClient", client);
        ReflectionTestUtils.setField(gateway, "properties", properties);
        queryGateway = new ElasticsearchTicketSearchQueryGateway();
        ReflectionTestUtils.setField(queryGateway, "elasticsearchClient", client);
        ReflectionTestUtils.setField(queryGateway, "properties", properties);
        TicketSearchProperties adminProperties = new TicketSearchProperties();
        adminProperties.setIndexAlias("ticket-search-alias-test");
        indexAdminGateway = new ElasticsearchTicketSearchIndexAdminGateway();
        ReflectionTestUtils.setField(indexAdminGateway, "elasticsearchClient", client);
        ReflectionTestUtils.setField(indexAdminGateway, "properties", adminProperties);
    }

    @AfterAll
    static void closeClient() throws Exception {
        if (transport != null) {
            transport.close();
        }
    }

    @Test
    @DisplayName("旧事件不能覆盖较新事件写入的文档")
    void shouldRejectOutOfOrderEvent() throws Exception {
        gateway.upsert(document(1L, 31L, "new"));

        assertThatThrownBy(() -> gateway.upsert(document(1L, 30L, "old")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("索引写入失败");

        TicketSearchDocument stored = client.get(request -> request.index(INDEX_NAME).id("1"),
                TicketSearchDocument.class).source();
        assertThat(stored).isNotNull();
        assertThat(stored.getSourceEventId()).isEqualTo(31L);
        assertThat(stored.getTitle()).isEqualTo("new");
    }

    @Test
    @DisplayName("关键词和状态过滤返回安全高亮候选")
    void shouldSearchWithKeywordAndFilter() throws Exception {
        TicketSearchDocument document = document(2L, 40L, "login <script> failed");
        document.setStatus("PROCESSING");
        document.setContent("login service unavailable");
        gateway.upsert(document);
        client.indices().refresh(request -> request.index(INDEX_NAME));
        TicketSearchQueryDTO query = new TicketSearchQueryDTO();
        query.setKeyword("login"); query.setStatus("PROCESSING");
        query.setSortBy("RELEVANCE"); query.setSortOrder("DESC");

        var page = queryGateway.search(query,
                new TicketAccessScope(1L, 1L, true, false, false, List.of()), null, 10);

        assertThat(page.candidates()).hasSize(1);
        assertThat(page.candidates().getFirst().document().getTicketId()).isEqualTo(2L);
        assertThat(page.candidates().getFirst().highlights()).anyMatch(value -> value.contains("\uE000login\uE001"));
    }

    @Test
    @DisplayName("版本化索引校验后可原子切换固定别名")
    void shouldSwitchVersionedIndexAlias() throws Exception {
        indexAdminGateway.createIndex("ticket-search-rebuild-a");
        gateway.upsertToIndex(document(101L, 1L, "old index"), "ticket-search-rebuild-a");
        indexAdminGateway.refreshIndex("ticket-search-rebuild-a");
        indexAdminGateway.switchAlias("ticket-search-rebuild-a");
        assertThat(indexAdminGateway.countDocuments("ticket-search-alias-test")).isEqualTo(1L);

        indexAdminGateway.createIndex("ticket-search-rebuild-b");
        gateway.upsertToIndex(document(102L, 2L, "new index"), "ticket-search-rebuild-b");
        indexAdminGateway.refreshIndex("ticket-search-rebuild-b");
        indexAdminGateway.switchAlias("ticket-search-rebuild-b");

        assertThat(indexAdminGateway.countDocuments("ticket-search-alias-test")).isEqualTo(1L);
        assertThat(client.exists(request -> request.index("ticket-search-alias-test").id("102")).value()).isTrue();
        assertThat(client.exists(request -> request.index("ticket-search-alias-test").id("101")).value()).isFalse();
    }

    @Test
    @DisplayName("首次重建可将旧实体索引原子迁移为固定别名")
    void shouldReplaceLegacyConcreteIndexWithAlias() throws Exception {
        String legacyIndexName = "ticket-search-legacy-test";
        TicketSearchProperties legacyProperties = new TicketSearchProperties();
        legacyProperties.setIndexAlias(legacyIndexName);
        ElasticsearchTicketSearchIndexAdminGateway legacyAdminGateway =
                new ElasticsearchTicketSearchIndexAdminGateway();
        ReflectionTestUtils.setField(legacyAdminGateway, "elasticsearchClient", client);
        ReflectionTestUtils.setField(legacyAdminGateway, "properties", legacyProperties);
        legacyAdminGateway.createIndex(legacyIndexName);
        gateway.upsertToIndex(document(201L, 1L, "legacy index"), legacyIndexName);
        legacyAdminGateway.createIndex("ticket-search-legacy-rebuild");
        gateway.upsertToIndex(document(202L, 2L, "rebuilt index"), "ticket-search-legacy-rebuild");
        legacyAdminGateway.refreshIndex("ticket-search-legacy-rebuild");

        legacyAdminGateway.switchAlias("ticket-search-legacy-rebuild");

        assertThat(client.indices().existsAlias(request -> request.name(legacyIndexName)).value()).isTrue();
        assertThat(client.exists(request -> request.index(legacyIndexName).id("202")).value()).isTrue();
        assertThat(client.exists(request -> request.index(legacyIndexName).id("201")).value()).isFalse();
    }

    private TicketSearchDocument document(Long ticketId, Long eventId, String title) {
        TicketSearchDocument document = new TicketSearchDocument();
        document.setTicketId(ticketId);
        document.setSourceEventId(eventId);
        document.setTitle(title);
        return document;
    }
}
