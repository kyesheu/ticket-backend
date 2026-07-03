package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketWorkflowBindDTO;
import com.ruoyi.ticket.dto.TicketWorkflowDefinitionDTO;
import com.ruoyi.ticket.service.ITicketWorkflowDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单流程定义 Controller
 *
 * @author ticket
 */
@RestController
@RequestMapping("/ticket/workflow")
public class TicketWorkflowDefinitionController extends BaseController {

    @Autowired
    private ITicketWorkflowDefinitionService workflowDefinitionService;

    @PreAuthorize("@ss.hasPermi('ticket:workflow:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(workflowDefinitionService.selectDefinitionList());
    }

    @PreAuthorize("@ss.hasPermi('ticket:workflow:query')")
    @GetMapping("/{definitionId}")
    public AjaxResult getInfo(@PathVariable Long definitionId) {
        return success(workflowDefinitionService.selectDefinitionById(definitionId));
    }

    @Log(title = "工单流程", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketWorkflowDefinitionDTO dto) {
        return success(workflowDefinitionService.insertDraft(dto));
    }

    @Log(title = "工单流程", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:edit')")
    @PutMapping("/{definitionId}")
    public AjaxResult edit(@PathVariable Long definitionId,
                           @Validated @RequestBody TicketWorkflowDefinitionDTO dto) {
        return toAjax(workflowDefinitionService.updateDraft(definitionId, dto));
    }

    @Log(title = "复制工单流程版本", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:add')")
    @PostMapping("/{definitionId}/copy")
    public AjaxResult copy(@PathVariable Long definitionId) {
        return success(workflowDefinitionService.copyVersion(definitionId));
    }

    @Log(title = "发布工单流程", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:publish')")
    @PutMapping("/{definitionId}/publish")
    public AjaxResult publish(@PathVariable Long definitionId) {
        return toAjax(workflowDefinitionService.publishDefinition(definitionId));
    }

    @Log(title = "绑定分类流程", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:workflow:edit')")
    @PutMapping("/bind-category")
    public AjaxResult bindCategory(@Validated @RequestBody TicketWorkflowBindDTO dto) {
        return toAjax(workflowDefinitionService.bindCategory(dto));
    }
}
