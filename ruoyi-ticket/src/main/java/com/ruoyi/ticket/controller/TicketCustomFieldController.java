package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketCustomFieldDefinitionDTO;
import com.ruoyi.ticket.service.ITicketCustomFieldDefinitionService;
import com.ruoyi.ticket.service.ITicketCustomFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 自定义字段定义管理 Controller。 */
@RestController
@RequestMapping("/ticket/custom-field")
public class TicketCustomFieldController extends BaseController {
    @Autowired private ITicketCustomFieldDefinitionService definitionService;
    @Autowired private ITicketCustomFieldService customFieldService;

    @PreAuthorize("@ss.hasPermi('ticket:custom-field:form')")
    @GetMapping("/form/{categoryId}")
    public AjaxResult form(@PathVariable Long categoryId) {
        return success(customFieldService.selectFormDefinitions(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('ticket:custom-field:list')")
    @GetMapping("/list/{categoryId}")
    public AjaxResult list(@PathVariable Long categoryId) { return success(definitionService.selectByCategoryId(categoryId)); }

    @PreAuthorize("@ss.hasPermi('ticket:custom-field:query')")
    @GetMapping("/{fieldId}")
    public AjaxResult getInfo(@PathVariable Long fieldId) { return success(definitionService.selectById(fieldId)); }

    @Log(title = "自定义字段", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:custom-field:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCustomFieldDefinitionDTO dto) {
        return success(definitionService.insertDefinition(dto));
    }

    @Log(title = "自定义字段", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:custom-field:edit')")
    @PutMapping("/{fieldId}")
    public AjaxResult edit(@PathVariable Long fieldId, @Validated @RequestBody TicketCustomFieldDefinitionDTO dto) {
        return toAjax(definitionService.updateDefinition(fieldId, dto));
    }
}
