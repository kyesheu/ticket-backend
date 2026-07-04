package com.ruoyi.ticket.service;

/** Elasticsearch 工单索引管理网关。 */
public interface TicketSearchIndexAdminGateway {
    void createIndex(String indexName);
    void refreshIndex(String indexName);
    long countDocuments(String indexName);
    void switchAlias(String indexName);
}
