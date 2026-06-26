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
import com.ruoyi.ticket.vo.TicketApiResponseVO;
import com.ruoyi.ticket.vo.TicketVO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 工单 Controller
 *
 * @author ticket
 */
@Tag(name = "工单管理", description = "工单的创建、分派、处理、确认、取消及查询")
@RestController
@RequestMapping("/ticket")
public class TicketController extends BaseController {

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ITicketCommentService ticketCommentService;

    @Autowired
    private ITicketOperationLogService ticketOperationLogService;

    @Operation(summary = "分页查询工单列表")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketPageResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:ticket:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketQueryDTO query) {
        startPage();
        return getDataTable(ticketService.selectTicketList(query));
    }

    @Operation(summary = "查询工单详情（含评论和操作日志）")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketDetailResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        TicketVO vo = ticketService.selectTicketById(id);
        vo.setComments(ticketCommentService.selectCommentsByTicketId(id));
        List<TicketOperationLog> logs = ticketOperationLogService.selectLogsByTicketId(id);
        vo.setLogs(logs);
        return success(vo);
    }

    @Operation(summary = "创建工单")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketIdResult.class)))
    @Log(title = "工单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCreateDTO dto) {
        return success(ticketService.createTicket(dto));
    }

    @Operation(summary = "分派工单")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PutMapping("/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @Validated @RequestBody TicketAssignDTO dto) {
        ticketService.assignTicket(id, dto);
        return success();
    }

    @Operation(summary = "处理工单")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:process')")
    @PutMapping("/{id}/process")
    public AjaxResult process(@PathVariable Long id, @Validated @RequestBody TicketProcessDTO dto) {
        ticketService.processTicket(id, dto);
        return success();
    }

    @Operation(summary = "确认工单（关闭）")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:confirm')")
    @PutMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id, @RequestBody TicketConfirmDTO dto) {
        ticketService.confirmTicket(id, dto);
        return success();
    }

    @Operation(summary = "取消工单")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:cancel')")
    @PutMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @Validated @RequestBody TicketCancelDTO dto) {
        ticketService.cancelTicket(id, dto);
        return success();
    }

    @Operation(summary = "查看工单操作日志")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketOperationLogListResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:log:list')")
    @GetMapping("/{ticketId}/logs")
    public AjaxResult logs(@PathVariable Long ticketId) {
        return success(ticketOperationLogService.selectLogsByTicketId(ticketId));
    }
}
