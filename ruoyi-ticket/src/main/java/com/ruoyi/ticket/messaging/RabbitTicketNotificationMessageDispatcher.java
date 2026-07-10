package com.ruoyi.ticket.messaging;

import com.ruoyi.ticket.config.TicketMessagingConfiguration;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 通过 RabbitMQ 异步投递站内通知。 */
@Component
@ConditionalOnProperty(prefix = "ticket.messaging", name = "enabled", havingValue = "true")
public class RabbitTicketNotificationMessageDispatcher implements TicketNotificationMessageDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitTicketNotificationMessageDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void dispatch(TicketNotificationMessage message) {
        rabbitTemplate.convertAndSend(TicketMessagingConfiguration.NOTIFICATION_EXCHANGE,
                TicketMessagingConfiguration.NOTIFICATION_ROUTING_KEY, message, rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return rabbitMessage;
                });
    }
}
