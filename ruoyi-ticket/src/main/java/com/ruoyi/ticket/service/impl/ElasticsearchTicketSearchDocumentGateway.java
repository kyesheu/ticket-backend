package com.ruoyi.ticket.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.service.TicketSearchDocumentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Elasticsearch 工单索引文档网关。
 */
@Component
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class ElasticsearchTicketSearchDocumentGateway implements TicketSearchDocumentGateway {

    @Autowired private ElasticsearchClient elasticsearchClient;
    @Autowired private TicketSearchProperties properties;

    @Override
    public void upsert(TicketSearchDocument document) {
        upsertToIndex(document, properties.getIndexAlias());
    }

    @Override
    public void upsertToIndex(TicketSearchDocument document, String indexName) {
        try {
            elasticsearchClient.index(request -> request
                    .index(indexName)
                    .id(String.valueOf(document.getTicketId()))
                    .version(document.getSourceEventId())
                    .versionType(VersionType.ExternalGte)
                    .document(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 工单索引写入失败", exception);
        }
    }
}
