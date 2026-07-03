package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.mapper.TicketWorkflowTaskMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketWorkflowTaskService;
import com.ruoyi.ticket.vo.TicketWorkflowTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 工单流程任务查询 Service 实现。 */
@Service
public class TicketWorkflowTaskServiceImpl implements ITicketWorkflowTaskService {

    private static final String TASK_PERMISSION = "ticket:workflow:task";

    @Autowired private TicketWorkflowTaskMapper taskMapper;
    @Autowired private ITicketAccessPolicy accessPolicy;

    @Override
    public List<TicketWorkflowTaskVO> selectMyPendingTasks() {
        return taskMapper.selectPendingTaskList(SecurityUtils.getUserId());
    }

    @Override
    public TicketWorkflowTaskVO selectTaskById(Long taskId) {
        TicketWorkflowTaskVO task = taskMapper.selectTaskDetail(taskId);
        if (task == null) throw new ServiceException("流程任务不存在");
        accessPolicy.assertCanAccess(task.getTicketId(), TASK_PERMISSION);
        return task;
    }
}
