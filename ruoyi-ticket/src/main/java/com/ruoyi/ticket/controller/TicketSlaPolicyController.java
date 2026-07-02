package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketSlaPolicyDTO;
import com.ruoyi.ticket.service.ITicketSlaPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 工单 SLA 策略 Controller
 *
 * @author ticket
 */
@Tag(name = "工单 SLA 策略管理", description = "查询和维护工单 SLA 策略")
@RestController
@RequestMapping("/ticket/sla")
public class TicketSlaPolicyController extends BaseController {

    @Autowired
    private ITicketSlaPolicyService ticketSlaPolicyService;

    @Operation(summary = "查询 SLA 策略列表")
    @PreAuthorize("@ss.hasPermi('ticket:sla:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(ticketSlaPolicyService.selectPolicyList());
    }

    @Operation(summary = "查询 SLA 策略详情")
    @PreAuthorize("@ss.hasPermi('ticket:sla:query')")
    @GetMapping("/{policyId}")
    public AjaxResult getInfo(@PathVariable Long policyId) {
        return success(ticketSlaPolicyService.selectPolicyById(policyId));
    }

    @Operation(summary = "新增 SLA 策略")
    @Log(title = "SLA 策略", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:sla:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketSlaPolicyDTO dto) {
        return toAjax(ticketSlaPolicyService.insertPolicy(dto));
    }

    @Operation(summary = "修改 SLA 策略")
    @Log(title = "SLA 策略", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:sla:edit')")
    @PutMapping("/{policyId}")
    public AjaxResult edit(@PathVariable Long policyId, @Validated @RequestBody TicketSlaPolicyDTO dto) {
        return toAjax(ticketSlaPolicyService.updatePolicy(policyId, dto));
    }
}
