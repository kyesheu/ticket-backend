package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/** 工单通知查询参数。 */
public class TicketNotificationQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String notificationType;
    private String readStatus;

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
}
