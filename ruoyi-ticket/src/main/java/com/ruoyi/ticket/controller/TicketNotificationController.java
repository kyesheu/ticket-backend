package com.ruoyi.ticket.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.service.ITicketNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工单通知 Controller。 */
@Tag(name = "工单站内通知", description = "查询当前用户通知并维护已读状态")
@RestController
@RequestMapping("/ticket/notification")
public class TicketNotificationController extends BaseController {

    @Autowired
    private ITicketNotificationService ticketNotificationService;

    @Operation(summary = "分页查询我的通知")
    @PreAuthorize("@ss.hasPermi('ticket:notification:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketNotificationQueryDTO query) {
        startPage();
        return getDataTable(ticketNotificationService.selectMyNotifications(query));
    }

    @Operation(summary = "查询我的未读通知数")
    @PreAuthorize("@ss.hasPermi('ticket:notification:list')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        return success(ticketNotificationService.countMyUnread());
    }

    @Operation(summary = "标记一条通知已读")
    @PreAuthorize("@ss.hasPermi('ticket:notification:read')")
    @PutMapping("/{notificationId}/read")
    public AjaxResult markRead(@PathVariable Long notificationId) {
        ticketNotificationService.markRead(notificationId);
        return success();
    }

    @Operation(summary = "标记全部通知已读")
    @PreAuthorize("@ss.hasPermi('ticket:notification:read')")
    @PutMapping("/read-all")
    public AjaxResult markAllRead() {
        ticketNotificationService.markAllRead();
        return success();
    }
}
