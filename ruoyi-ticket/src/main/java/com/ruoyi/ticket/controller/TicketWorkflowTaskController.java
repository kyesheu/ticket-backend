package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketWorkflowTaskActionDTO;
import com.ruoyi.ticket.service.ITicketWorkflowEngine;
import com.ruoyi.ticket.service.ITicketWorkflowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单流程任务 Controller
 *
 * @author ticket
 */
@RestController
@RequestMapping("/ticket/workflow/task")
public class TicketWorkflowTaskController extends BaseController {

    @Autowired
    private ITicketWorkflowEngine workflowEngine;

    @Autowired
    private ITicketWorkflowTaskService workflowTaskService;

    @PreAuthorize("@ss.hasPermi('ticket:workflow:task')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(workflowTaskService.selectMyPendingTasks());
    }

    @PreAuthorize("@ss.hasPermi('ticket:workflow:task')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId) {
        return success(workflowTaskService.selectTaskById(taskId));
    }

    @Log(title = "完成流程任务", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:task')")
    @PutMapping("/{taskId}/complete")
    public AjaxResult complete(@PathVariable Long taskId, @RequestBody TicketWorkflowTaskActionDTO dto) {
        workflowEngine.completeTask(taskId, dto);
        return success();
    }

    @Log(title = "退回流程任务", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:task')")
    @PutMapping("/{taskId}/return")
    public AjaxResult returnTask(@PathVariable Long taskId, @RequestBody TicketWorkflowTaskActionDTO dto) {
        workflowEngine.returnTask(taskId, dto);
        return success();
    }

    @Log(title = "取消流程实例", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:task')")
    @PutMapping("/ticket/{ticketId}/cancel")
    public AjaxResult cancel(@PathVariable Long ticketId, @RequestBody TicketWorkflowTaskActionDTO dto) {
        workflowEngine.cancelInstance(ticketId, dto.getComment());
        return success();
    }

    @Log(title = "终止流程实例", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:publish')")
    @PutMapping("/ticket/{ticketId}/terminate")
    public AjaxResult terminate(@PathVariable Long ticketId, @RequestBody TicketWorkflowTaskActionDTO dto) {
        workflowEngine.terminateInstance(ticketId, dto.getComment());
        return success();
    }
}
