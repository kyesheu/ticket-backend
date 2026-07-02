package com.ruoyi.ticket.enums;

/**
 * 工单 SLA 告警类型
 *
 * @author ticket
 */
public enum TicketSlaAlertType {

    /** 首次响应超时 */
    RESPONSE_OVERDUE("首次响应超时"),

    /** 解决超时 */
    RESOLUTION_OVERDUE("解决超时");

    private final String label;

    TicketSlaAlertType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
