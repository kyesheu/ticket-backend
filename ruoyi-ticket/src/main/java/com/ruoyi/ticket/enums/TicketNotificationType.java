package com.ruoyi.ticket.enums;

/** 工单通知类型。 */
public enum TicketNotificationType {
    ASSIGNED("工单已分派"),
    PROCESSED("工单已处理"),
    CLOSED("工单已关闭"),
    CANCELLED("工单已取消"),
    COMMENTED("工单有新评论"),
    SLA_OVERDUE("工单 SLA 超时");

    private final String label;

    TicketNotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
