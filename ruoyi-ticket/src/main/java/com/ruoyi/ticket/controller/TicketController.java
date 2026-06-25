package com.ruoyi.ticket.controller;

import java.util.List;
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
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.service.ITicketCommentService;
import com.ruoyi.ticket.service.ITicketOperationLogService;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketVO;

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

    /**
     * 分页查询工单列表
     */
    @PreAuthorize("@ss.hasPermi('ticket:ticket:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketQueryDTO query) {
        startPage();
        return getDataTable(ticketService.selectTicketList(query));
    }

    /**
     * 查询工单详情（含评论和操作日志）
     */
    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        TicketVO vo = ticketService.selectTicketById(id);
        // 附加评论列表
        List<TicketComment> comments = ticketCommentService.selectCommentsByTicketId(id);
        vo.setComments(comments);
        // 附加操作日志
        List<TicketOperationLog> logs = ticketOperationLogService.selectLogsByTicketId(id);
        vo.setLogs(logs);
        return success(vo);
    }

    /**
     * 创建工单
     */
    @Log(title = "工单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCreateDTO dto) {
        Long ticketId = ticketService.createTicket(dto);
        return success(ticketId);
    }

    /**
     * 分派工单
     */
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PutMapping("/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @Validated @RequestBody TicketAssignDTO dto) {
        ticketService.assignTicket(id, dto);
        return success();
    }

    /**
     * 处理工单
     */
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:process')")
    @PutMapping("/{id}/process")
    public AjaxResult process(@PathVariable Long id, @Validated @RequestBody TicketProcessDTO dto) {
        ticketService.processTicket(id, dto);
        return success();
    }

    /**
     * 确认工单
     */
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:confirm')")
    @PutMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id, @RequestBody TicketConfirmDTO dto) {
        ticketService.confirmTicket(id, dto);
        return success();
    }

    /**
     * 取消工单
     */
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:cancel')")
    @PutMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @Validated @RequestBody TicketCancelDTO dto) {
        ticketService.cancelTicket(id, dto);
        return success();
    }

    /**
     * 查看操作日志
     */
    @PreAuthorize("@ss.hasPermi('ticket:log:list')")
    @GetMapping("/{ticketId}/logs")
    public AjaxResult logs(@PathVariable Long ticketId) {
        List<TicketOperationLog> logs = ticketOperationLogService.selectLogsByTicketId(ticketId);
        return success(logs);
    }

}
