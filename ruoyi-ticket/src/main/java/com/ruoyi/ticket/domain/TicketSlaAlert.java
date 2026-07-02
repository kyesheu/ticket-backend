package com.ruoyi.ticket.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单 SLA 告警实体
 * 告警是不可变事实记录，不继承 BaseEntity
 *
 * @author ticket
 */
public class TicketSlaAlert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 告警 ID */
    private Long alertId;

    /** 工单 ID */
    private Long ticketId;

    /** 告警类型 */
    private String alertType;

    /** SLA 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueAt;

    /** 发现超时的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date detectedAt;

    /** 发现时已超时分钟数 */
    private Integer overdueMinutes;

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    public Date getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Date detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Integer getOverdueMinutes() {
        return overdueMinutes;
    }

    public void setOverdueMinutes(Integer overdueMinutes) {
        this.overdueMinutes = overdueMinutes;
    }
}
