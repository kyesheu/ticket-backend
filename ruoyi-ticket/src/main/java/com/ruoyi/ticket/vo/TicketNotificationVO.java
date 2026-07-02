package com.ruoyi.ticket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 工单通知响应对象。 */
public class TicketNotificationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long notificationId;
    private Long ticketId;
    private String ticketNo;
    private String ticketTitle;
    private String notificationType;
    private String title;
    private String content;
    private String readStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTicketTitle() { return ticketTitle; }
    public void setTicketTitle(String ticketTitle) { this.ticketTitle = ticketTitle; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
