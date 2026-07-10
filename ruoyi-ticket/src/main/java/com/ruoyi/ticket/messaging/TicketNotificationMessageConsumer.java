package com.ruoyi.ticket.messaging;

import com.ruoyi.ticket.config.TicketMessagingConfiguration;
import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.mapper.TicketNotificationMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 消费通知消息并持久化为站内通知。 */
@Component
@ConditionalOnProperty(prefix = "ticket.messaging", name = "enabled", havingValue = "true")
public class TicketNotificationMessageConsumer {

    private final TicketNotificationMapper ticketNotificationMapper;

    public TicketNotificationMessageConsumer(TicketNotificationMapper ticketNotificationMapper) {
        this.ticketNotificationMapper = ticketNotificationMapper;
    }

    @RabbitListener(queues = TicketMessagingConfiguration.NOTIFICATION_QUEUE,
            containerFactory = "ticketRabbitListenerContainerFactory")
    public void consume(TicketNotificationMessage message) {
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
