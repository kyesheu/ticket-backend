package com.ruoyi.ticket.messaging;

import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.mapper.TicketNotificationMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 未启用 RabbitMQ 时直接写入站内通知，保证本地开发可用。 */
@Component
@ConditionalOnProperty(prefix = "ticket.messaging", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DatabaseTicketNotificationMessageDispatcher implements TicketNotificationMessageDispatcher {

    private final TicketNotificationMapper ticketNotificationMapper;

    public DatabaseTicketNotificationMessageDispatcher(TicketNotificationMapper ticketNotificationMapper) {
        this.ticketNotificationMapper = ticketNotificationMapper;
    }

    @Override
    public void dispatch(TicketNotificationMessage message) {
        TicketNotification notification = new TicketNotification();
        notification.setTicketId(message.ticketId());
        notification.setRecipientId(message.recipientId());
        notification.setNotificationType(message.notificationType());
        notification.setEventKey(message.eventKey());
        notification.setTitle(message.title());
        notification.setContent(message.content());
        notification.setReadStatus("0");
        notification.setCreateTime(message.createTime());
        ticketNotificationMapper.insertNotification(notification);
    }
}
