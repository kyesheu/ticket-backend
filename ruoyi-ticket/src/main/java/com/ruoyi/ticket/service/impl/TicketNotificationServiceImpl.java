package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.mapper.TicketNotificationMapper;
import com.ruoyi.ticket.messaging.TicketNotificationMessage;
import com.ruoyi.ticket.messaging.TicketNotificationMessageDispatcher;
import com.ruoyi.ticket.service.ITicketNotificationService;
import com.ruoyi.ticket.vo.TicketNotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Date;
import java.util.Objects;

/** 工单通知 Service 实现。 */
@Service
public class TicketNotificationServiceImpl implements ITicketNotificationService {

    private static final String READ_STATUS = "1";

    private final TicketNotificationMapper ticketNotificationMapper;
    private final TicketNotificationMessageDispatcher messageDispatcher;

    public TicketNotificationServiceImpl(TicketNotificationMapper ticketNotificationMapper,
                                         TicketNotificationMessageDispatcher messageDispatcher) {
        this.ticketNotificationMapper = ticketNotificationMapper;
        this.messageDispatcher = messageDispatcher;
    }

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
        TicketNotificationMessage message = new TicketNotificationMessage(ticketId, recipientId,
                type.name(), eventKey, title, content, new Date());
        dispatchAfterCommit(message);
        return 1;
    }

    private void dispatchAfterCommit(TicketNotificationMessage message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messageDispatcher.dispatch(message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageDispatcher.dispatch(message);
            }
        });
    }
}
