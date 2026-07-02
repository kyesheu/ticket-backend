package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketSlaAlertQueryDTO;
import com.ruoyi.ticket.service.ITicketSlaAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工单 SLA 告警 Controller。 */
@Tag(name = "工单 SLA 告警", description = "查询 SLA 告警和手工触发超时扫描")
@RestController
@RequestMapping("/ticket/sla-alert")
public class TicketSlaAlertController extends BaseController {

    @Autowired
    private ITicketSlaAlertService ticketSlaAlertService;

    @Operation(summary = "分页查询 SLA 告警")
    @PreAuthorize("@ss.hasPermi('ticket:sla-alert:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketSlaAlertQueryDTO query) {
        startPage();
        return getDataTable(ticketSlaAlertService.selectAlertList(query));
    }

    @Operation(summary = "查询 SLA 告警详情")
    @PreAuthorize("@ss.hasPermi('ticket:sla-alert:query')")
    @GetMapping("/{alertId}")
    public AjaxResult getInfo(@PathVariable Long alertId) {
        return success(ticketSlaAlertService.selectAlertById(alertId));
    }

    @Operation(summary = "手工触发 SLA 超时扫描")
    @Log(title = "SLA 超时扫描", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:sla-alert:scan')")
    @PostMapping("/scan")
    public AjaxResult scan() {
        return success(ticketSlaAlertService.scanOverdue());
    }
}
