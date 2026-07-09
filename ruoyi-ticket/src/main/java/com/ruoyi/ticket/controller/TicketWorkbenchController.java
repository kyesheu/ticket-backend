package com.ruoyi.ticket.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.service.ITicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理人工作台。
 */
@Tag(name = "处理人工作台", description = "处理人待办工单聚合入口")
@RestController
@RequestMapping("/ticket/workbench")
public class TicketWorkbenchController extends BaseController {

    @Autowired
    private ITicketService ticketService;

    @Operation(summary = "查询我的待办工单")
    @PreAuthorize("@ss.hasPermi('ticket:workbench:list')")
    @GetMapping("/my-todo")
    public TableDataInfo myTodo(TicketQueryDTO query) {
        startPage();
        return getDataTable(ticketService.selectMyTodoTickets(query));
    }
}
