package com.ruoyi.ticket.service;

import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.dto.TicketWorkflowTaskActionDTO;

/**
 * 工单流程引擎接口
 *
 * @author ticket
 */
public interface ITicketWorkflowEngine {

    /**
     * 为新工单锁定流程版本并启动实例。
     *
     * @param ticket 已持久化的工单
     * @return 流程实例 ID
     */
    Long startInstance(Ticket ticket);

    void completeTask(Long taskId, TicketWorkflowTaskActionDTO dto);

    void completeCurrentTask(Long ticketId, TicketWorkflowTaskActionDTO dto);

    void returnTask(Long taskId, TicketWorkflowTaskActionDTO dto);

    void cancelInstance(Long ticketId, String comment);

    void terminateInstance(Long ticketId, String comment);
}
