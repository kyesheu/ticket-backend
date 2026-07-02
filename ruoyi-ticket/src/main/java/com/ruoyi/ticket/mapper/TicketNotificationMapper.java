package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketNotification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 工单通知 Mapper。 */
public interface TicketNotificationMapper {
    List<TicketNotification> selectNotificationsByRecipientId(Long recipientId);
    int countUnreadByRecipientId(Long recipientId);
    int insertNotification(TicketNotification notification);
    int markRead(@Param("notificationId") Long notificationId, @Param("recipientId") Long recipientId);
    int markAllRead(Long recipientId);
}
