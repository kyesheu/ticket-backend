package com.ruoyi.ticket.controller;

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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.service.ITicketCommentService;
import com.ruoyi.ticket.service.ITicketOperationLogService;
import com.ruoyi.ticket.service.ITicketService;

/**
 * 工单 Controller
 *
 * @author ticket
 */
@RestController
@RequestMapping("/ticket")
public class TicketController extends BaseController {

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ITicketCommentService ticketCommentService;

    @Autowired
    private ITicketOperationLogService ticketOperationLogService;

    @PreAuthorize("@ss.hasPermi('ticket:ticket:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketQueryDTO query) {
        // TODO: 阶段五实现
        return getDataTable(null);
    }

    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        // TODO: 阶段五实现
        return success();
    }

    @Log(title = "工单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCreateDTO dto) {
        // TODO: 阶段五实现
        return success();
    }

    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PutMapping("/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @Validated @RequestBody TicketAssignDTO dto) {
        // TODO: 阶段五实现
        return success();
    }

    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:process')")
    @PutMapping("/{id}/process")
    public AjaxResult process(@PathVariable Long id, @Validated @RequestBody TicketProcessDTO dto) {
        // TODO: 阶段五实现
        return success();
    }

    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:confirm')")
    @PutMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id, @RequestBody TicketConfirmDTO dto) {
        // TODO: 阶段五实现
        return success();
    }

    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:cancel')")
    @PutMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @Validated @RequestBody TicketCancelDTO dto) {
        // TODO: 阶段五实现
        return success();
    }

    @PreAuthorize("@ss.hasPermi('ticket:log:list')")
    @GetMapping("/{ticketId}/logs")
    public AjaxResult logs(@PathVariable Long ticketId) {
        // TODO: 阶段六实现
        return success();
    }
}
