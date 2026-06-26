package com.ruoyi.ticket.enums;

/**
 * 工单操作类型枚举
 * 对应 ticket_operation_log.operation_type
 *
 * @author ticket
 */
public enum TicketOperationType {

    CREATE("创建"),
    ASSIGN("分派"),
    PROCESS("处理"),
    CONFIRM("确认"),
    CANCEL("取消");

    private final String label;

    TicketOperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
