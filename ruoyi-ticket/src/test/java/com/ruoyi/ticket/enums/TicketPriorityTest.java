package com.ruoyi.ticket.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TicketPriority 优先级枚举单元测试
 *
 * @author ticket
 */
@DisplayName("工单优先级测试")
class TicketPriorityTest {

    @Test
    @DisplayName("枚举包含全部 4 个值")
    void shouldContainFourValues() {
        TicketPriority[] values = TicketPriority.values();
        assertThat(values).hasSize(4);
        assertThat(values).containsExactly(
                TicketPriority.LOW, TicketPriority.MEDIUM,
                TicketPriority.HIGH, TicketPriority.URGENT);
    }

    @Test
    @DisplayName("每个值都有中文标签")
    void shouldHaveChineseLabels() {
        assertThat(TicketPriority.LOW.getLabel()).isEqualTo("低");
        assertThat(TicketPriority.MEDIUM.getLabel()).isEqualTo("中");
        assertThat(TicketPriority.HIGH.getLabel()).isEqualTo("高");
        assertThat(TicketPriority.URGENT.getLabel()).isEqualTo("紧急");
    }

    @Test
    @DisplayName("name() 返回英文大写")
    void nameShouldReturnUppercase() {
        assertThat(TicketPriority.LOW.name()).isEqualTo("LOW");
        assertThat(TicketPriority.MEDIUM.name()).isEqualTo("MEDIUM");
        assertThat(TicketPriority.HIGH.name()).isEqualTo("HIGH");
        assertThat(TicketPriority.URGENT.name()).isEqualTo("URGENT");
    }
}
