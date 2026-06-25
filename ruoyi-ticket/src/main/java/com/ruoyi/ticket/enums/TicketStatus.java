package com.ruoyi.ticket.enums;

import java.util.Collections;
import java.util.Set;

/**
 * 工单状态枚举
 * 状态流转规则内置在 allowed 集合中，Service 层统一调用 {@link #canTransitionTo(TicketStatus)}
 *
 * <pre>
 * NEW ──分派──▶ PROCESSING ──处理──▶ WAIT_CONFIRM ──确认──▶ CLOSED
 *  │                  │
 *  └──取消────────────┘
 * </pre>
 *
 * @author ticket
 */
public enum TicketStatus {

    NEW("待分派"),
    PROCESSING("处理中"),
    WAIT_CONFIRM("待确认"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String label;

    /** 当前状态允许转入的目标状态集合 */
    private Set<TicketStatus> allowed = Collections.emptySet();

    TicketStatus(String label) {
        this.label = label;
    }

    static {
        NEW.allowed = Set.of(PROCESSING, CANCELLED);
        PROCESSING.allowed = Set.of(WAIT_CONFIRM, CANCELLED);
        WAIT_CONFIRM.allowed = Set.of(CLOSED);
        // CLOSED 和 CANCELLED 为终态，allowed 保持空集合
    }

    public String getLabel() {
        return label;
    }

    /**
     * 判断是否可以流转到目标状态
     */
    public boolean canTransitionTo(TicketStatus target) {
        return this.allowed.contains(target);
    }

    /**
     * 获取当前状态允许转入的所有目标状态（只读）
     */
    public Set<TicketStatus> allowedTransitions() {
        return Collections.unmodifiableSet(this.allowed);
    }
}
