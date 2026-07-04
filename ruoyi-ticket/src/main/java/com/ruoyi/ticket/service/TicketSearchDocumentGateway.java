package com.ruoyi.ticket.service;

import com.ruoyi.ticket.domain.TicketSearchDocument;

/**
 * 工单索引文档外部存储网关。
 */
public interface TicketSearchDocumentGateway {
    void upsert(TicketSearchDocument document);

    void upsertToIndex(TicketSearchDocument document, String indexName);
}
