package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.mapper.TicketNotificationMapper;
import com.ruoyi.ticket.service.ITicketNotificationService;
import com.ruoyi.ticket.vo.TicketNotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Date;
import java.util.Objects;

/** 工单通知 Service 实现。 */
@Service
public class TicketNotificationServiceImpl implements ITicketNotificationService {

    private static final String READ_STATUS = "1";

    @Autowired
    private TicketNotificationMapper ticketNotificationMapper;

    @Override
    public List<TicketNotificationVO> selectMyNotifications(TicketNotificationQueryDTO query) {
        return ticketNotificationMapper.selectNotificationList(SecurityUtils.getUserId(), query);
    }

    @Override
    public int countMyUnread() {
        return ticketNotificationMapper.countUnreadByRecipientId(SecurityUtils.getUserId());
    }

    @Override
    public void markRead(Long notificationId) {
        Long userId = SecurityUtils.getUserId();
        TicketNotification notification = ticketNotificationMapper.selectNotificationById(notificationId, userId);
        if (notification == null) {
            throw new ServiceException("通知不存在");
        }
        if (!READ_STATUS.equals(notification.getReadStatus())) {
            ticketNotificationMapper.markRead(notificationId, userId);
        }
    }

    @Override
    public void markAllRead() {
        ticketNotificationMapper.markAllRead(SecurityUtils.getUserId());
    }

    @Override
    public int createNotification(Long ticketId, Long recipientId, Long operatorId,
                                  TicketNotificationType type, String eventKey,
                                  String title, String content) {
        if (recipientId == null || Objects.equals(recipientId, operatorId)) {
            return 0;
        }
        TicketNotification notification = new TicketNotification();
        notification.setTicketId(ticketId);
        notification.setRecipientId(recipientId);
        notification.setNotificationType(type.name());
        notification.setEventKey(eventKey);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadStatus("0");
        notification.setCreateTime(new Date());
        return ticketNotificationMapper.insertNotification(notification);
    }
}
