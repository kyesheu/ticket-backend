package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketWorkflowInstance;

/**
 * 工单流程Instance Mapper 接口
 *
 * @author ticket
 */
public interface TicketWorkflowInstanceMapper {

    /**
     * 根据工单查询流程实例
     *
     * @param ticketId 根据工单查询流程实例
     * @return 查询结果
     */
    TicketWorkflowInstance selectInstanceByTicketId(Long ticketId);

    /**
     * 新增流程实例
     *
     * @param instance 流程实例
     * @return 影响行数
     */
    int insertInstance(TicketWorkflowInstance instance);

    TicketWorkflowInstance selectInstanceById(Long instanceId);

    int updateInstance(TicketWorkflowInstance instance);
}
