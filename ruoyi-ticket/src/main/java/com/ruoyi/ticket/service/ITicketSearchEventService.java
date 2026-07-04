package com.ruoyi.ticket.service;

/**
 * 工单检索事务事件服务。
 */
public interface ITicketSearchEventService {

    /**
     * 发布工单索引更新事件。
     *
     * @param ticketId 工单 ID
     */
    void publishUpsert(Long ticketId);
}
