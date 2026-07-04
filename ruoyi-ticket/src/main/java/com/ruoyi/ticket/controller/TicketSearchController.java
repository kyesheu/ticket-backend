package com.ruoyi.ticket.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.service.ITicketSearchService;
import com.ruoyi.ticket.service.ITicketSearchRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

/** 工单全文检索接口。 */
@Tag(name = "工单检索", description = "Elasticsearch 工单全文检索")
@RestController
@RequestMapping("/ticket/search")
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchController extends BaseController {

    @Autowired private ITicketSearchService ticketSearchService;
    @Autowired private ITicketSearchRebuildService ticketSearchRebuildService;

    @Operation(summary = "全文检索工单")
    @PreAuthorize("@ss.hasPermi('ticket:search:query')")
    @GetMapping
    public AjaxResult search(TicketSearchQueryDTO query) {
        return success(ticketSearchService.search(query));
    }

    @Operation(summary = "触发全量索引重建")
    @PreAuthorize("@ss.hasPermi('ticket:search:rebuild')")
    @PostMapping("/rebuild")
    public AjaxResult rebuild() {
        ticketSearchRebuildService.startRebuild();
        return success();
    }

    @Operation(summary = "查询全量索引重建进度")
    @PreAuthorize("@ss.hasPermi('ticket:search:rebuild')")
    @GetMapping("/rebuild")
    public AjaxResult rebuildStatus() {
        return success(ticketSearchRebuildService.getStatus());
    }

    @Operation(summary = "重试失败的检索事件")
    @PreAuthorize("@ss.hasPermi('ticket:search:retry')")
    @PostMapping("/events/retry")
    public AjaxResult retryFailedEvents() {
        return success(ticketSearchRebuildService.retryFailedEvents());
    }
}
