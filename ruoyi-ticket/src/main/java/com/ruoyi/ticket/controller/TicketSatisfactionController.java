package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketSatisfactionCreateDTO;
import com.ruoyi.ticket.dto.TicketSatisfactionQueryDTO;
import com.ruoyi.ticket.service.ITicketSatisfactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工单满意度 Controller。 */
@Tag(name = "工单满意度", description = "提交和查询工单满意度评价")
@RestController
@RequestMapping("/ticket/satisfaction")
public class TicketSatisfactionController extends BaseController {

    @Autowired
    private ITicketSatisfactionService ticketSatisfactionService;

    @Operation(summary = "提交工单评价")
    @Log(title = "工单满意度", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:satisfaction:add')")
    @PostMapping("/{ticketId}")
    public AjaxResult add(@PathVariable Long ticketId,
                          @Validated @RequestBody TicketSatisfactionCreateDTO dto) {
        return success(ticketSatisfactionService.createSatisfaction(ticketId, dto));
    }

    @Operation(summary = "查询工单评价")
    @PreAuthorize("@ss.hasPermi('ticket:satisfaction:query')")
    @GetMapping("/ticket/{ticketId}")
    public AjaxResult getByTicketId(@PathVariable Long ticketId) {
        return success(ticketSatisfactionService.selectByTicketId(ticketId));
    }

    @Operation(summary = "分页查询评价列表")
    @PreAuthorize("@ss.hasPermi('ticket:satisfaction:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketSatisfactionQueryDTO query) {
        startPage();
        return getDataTable(ticketSatisfactionService.selectSatisfactionList(query));
    }

    @Operation(summary = "查询满意度统计")
    @PreAuthorize("@ss.hasPermi('ticket:satisfaction:statistics')")
    @GetMapping("/statistics")
    public AjaxResult statistics(TicketSatisfactionQueryDTO query) {
        return success(ticketSatisfactionService.selectStatistics(query));
    }
}
