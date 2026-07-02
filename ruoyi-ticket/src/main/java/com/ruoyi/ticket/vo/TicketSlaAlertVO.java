package com.ruoyi.ticket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单 SLA 告警响应对象
 *
 * @author ticket
 */
public class TicketSlaAlertVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long alertId;
    private Long ticketId;
    private String ticketNo;
    private String title;
    private String priority;
    private String ticketStatus;
    private String alertType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date detectedAt;

    private Integer overdueMinutes;

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getTicketStatus() { return ticketStatus; }
    public void setTicketStatus(String ticketStatus) { this.ticketStatus = ticketStatus; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public Date getDueAt() { return dueAt; }
    public void setDueAt(Date dueAt) { this.dueAt = dueAt; }
    public Date getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Date detectedAt) { this.detectedAt = detectedAt; }
    public Integer getOverdueMinutes() { return overdueMinutes; }
    public void setOverdueMinutes(Integer overdueMinutes) { this.overdueMinutes = overdueMinutes; }
}
