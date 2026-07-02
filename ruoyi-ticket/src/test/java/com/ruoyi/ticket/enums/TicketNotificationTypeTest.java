package com.ruoyi.ticket.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 工单通知类型测试。 */
@DisplayName("工单通知类型测试")
class TicketNotificationTypeTest {
    @Test
    @DisplayName("包含全部六类通知事件")
    void shouldContainAllNotificationTypes() {
        assertThat(TicketNotificationType.values()).containsExactly(
                TicketNotificationType.ASSIGNED, TicketNotificationType.PROCESSED,
                TicketNotificationType.CLOSED, TicketNotificationType.CANCELLED,
                TicketNotificationType.COMMENTED, TicketNotificationType.SLA_OVERDUE);
        assertThat(TicketNotificationType.values()).allMatch(type -> !type.getLabel().isBlank());
    }
}
