package com.ruoyi.ticket.service;

import com.ruoyi.ticket.vo.TicketWorkflowTaskVO;

import java.util.List;

/** 工单流程任务查询 Service。 */
public interface ITicketWorkflowTaskService {
    List<TicketWorkflowTaskVO> selectMyPendingTasks();
    TicketWorkflowTaskVO selectTaskById(Long taskId);
}
