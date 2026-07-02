package com.ruoyi.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单 SLA 告警查询参数
 *
 * @author ticket
 */
public class TicketSlaAlertQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "工单编号")
    private String ticketNo;

    @Schema(description = "发现时间起点")
    private Date beginDetectedAt;

    @Schema(description = "发现时间终点")
    private Date endDetectedAt;

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Date getBeginDetectedAt() { return beginDetectedAt; }
    public void setBeginDetectedAt(Date beginDetectedAt) { this.beginDetectedAt = beginDetectedAt; }
    public Date getEndDetectedAt() { return endDetectedAt; }
    public void setEndDetectedAt(Date endDetectedAt) { this.endDetectedAt = endDetectedAt; }
}
