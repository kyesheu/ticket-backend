package com.ruoyi.ticket.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单 SLA 告警类型测试
 *
 * @author ticket
 */
@DisplayName("工单 SLA 告警类型测试")
class TicketSlaAlertTypeTest {

    @Test
    @DisplayName("枚举包含响应超时和解决超时")
    void shouldContainAllAlertTypes() {
        assertThat(TicketSlaAlertType.values()).containsExactly(
                TicketSlaAlertType.RESPONSE_OVERDUE,
                TicketSlaAlertType.RESOLUTION_OVERDUE);
    }

    @Test
    @DisplayName("每种告警类型都有中文标签")
    void shouldHaveChineseLabels() {
        assertThat(TicketSlaAlertType.RESPONSE_OVERDUE.getLabel()).isEqualTo("首次响应超时");
        assertThat(TicketSlaAlertType.RESOLUTION_OVERDUE.getLabel()).isEqualTo("解决超时");
    }
}
