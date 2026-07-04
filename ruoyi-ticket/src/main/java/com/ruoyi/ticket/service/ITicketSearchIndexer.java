package com.ruoyi.ticket.service;

/**
 * 工单检索索引器。
 */
public interface ITicketSearchIndexer {

    /**
     * 重新读取 MySQL 快照并更新工单索引。
     *
     * @param ticketId 工单 ID
     * @param sourceEventId 来源事件 ID
     */
    void upsertTicket(Long ticketId, Long sourceEventId);

    void indexTicketTo(Long ticketId, Long sourceEventId, String indexName);
}
