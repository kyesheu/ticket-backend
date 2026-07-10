package com.ruoyi.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 工单 RabbitMQ 队列与消费者配置。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TicketMessagingProperties.class)
@ConditionalOnProperty(prefix = "ticket.messaging", name = "enabled", havingValue = "true")
public class TicketMessagingConfiguration {

    public static final String NOTIFICATION_EXCHANGE = "ticket.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "ticket.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "ticket.notification.created";
    public static final String NOTIFICATION_DLX = "ticket.notification.dlx";
    public static final String NOTIFICATION_DLQ = "ticket.notification.dlq";
    public static final String NOTIFICATION_DLK = "ticket.notification.dead";

    @Bean
    public DirectExchange ticketNotificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange ticketNotificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue ticketNotificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLK)
                .build();
    }

    @Bean
    public Queue ticketNotificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding ticketNotificationBinding() {
        return BindingBuilder.bind(ticketNotificationQueue())
                .to(ticketNotificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding ticketNotificationDeadLetterBinding() {
        return BindingBuilder.bind(ticketNotificationDeadLetterQueue())
                .to(ticketNotificationDeadLetterExchange())
                .with(NOTIFICATION_DLK);
    }

    @Bean
    public MessageConverter ticketRabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory ticketRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter ticketRabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(ticketRabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
