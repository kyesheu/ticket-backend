package com.ruoyi.ticket.messaging;

import java.io.Serializable;
import java.util.Date;

/** 站内通知异步投递消息。 */
public record TicketNotificationMessage(Long ticketId, Long recipientId, String notificationType,
                                        String eventKey, String title, String content,
                                        Date createTime) implements Serializable {
}
