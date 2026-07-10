package com.ruoyi.ticket.messaging;

/** 通知投递通道，支持 RabbitMQ 与本地降级实现。 */
public interface TicketNotificationMessageDispatcher {

    void dispatch(TicketNotificationMessage message);
}
