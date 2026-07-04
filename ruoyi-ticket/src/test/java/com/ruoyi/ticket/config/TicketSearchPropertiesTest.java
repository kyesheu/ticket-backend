package com.ruoyi.ticket.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单检索配置测试。
 */
@DisplayName("工单检索配置测试")
class TicketSearchPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TicketSearchConfiguration.class);

    @Test
    @DisplayName("默认关闭 Elasticsearch 检索")
    void shouldDisableSearchByDefault() {
        TicketSearchProperties properties = new TicketSearchProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getUris()).containsExactly("http://localhost:9200");
        assertThat(properties.getIndexAlias()).isEqualTo("ticket-search");
    }

    @Test
    @DisplayName("关闭检索时不创建 Elasticsearch 客户端")
    void shouldNotCreateClientWhenSearchIsDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ElasticsearchClient.class));
    }

    @Test
    @DisplayName("启用检索时创建 Elasticsearch 客户端")
    void shouldCreateClientWhenSearchIsEnabled() {
        contextRunner.withPropertyValues("ticket.search.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ElasticsearchClient.class));
    }
}
