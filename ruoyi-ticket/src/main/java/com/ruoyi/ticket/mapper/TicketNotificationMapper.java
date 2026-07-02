package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.vo.TicketNotificationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 工单通知 Mapper。 */
public interface TicketNotificationMapper {
    List<TicketNotificationVO> selectNotificationList(@Param("recipientId") Long recipientId,
                                                       @Param("query") TicketNotificationQueryDTO query);
    TicketNotification selectNotificationById(@Param("notificationId") Long notificationId,
                                                @Param("recipientId") Long recipientId);
    int countUnreadByRecipientId(Long recipientId);
    int insertNotification(TicketNotification notification);
    int markRead(@Param("notificationId") Long notificationId, @Param("recipientId") Long recipientId);
    int markAllRead(Long recipientId);
}
