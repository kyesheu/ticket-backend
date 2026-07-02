package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.vo.TicketNotificationVO;

import java.util.List;

/** 工单通知 Service 接口。 */
public interface ITicketNotificationService {
    List<TicketNotificationVO> selectMyNotifications(TicketNotificationQueryDTO query);
    int countMyUnread();
    void markRead(Long notificationId);
    void markAllRead();
}
