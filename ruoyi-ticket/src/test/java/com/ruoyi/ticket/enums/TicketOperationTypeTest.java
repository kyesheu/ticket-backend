package com.ruoyi.ticket.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TicketOperationType 操作类型枚举单元测试
 *
 * @author ticket
 */
@DisplayName("工单操作类型测试")
class TicketOperationTypeTest {

    @Test
    @DisplayName("枚举包含全部 5 个操作类型")
    void shouldContainFiveValues() {
        TicketOperationType[] values = TicketOperationType.values();
        assertThat(values).hasSize(5);
        assertThat(values).containsExactly(
                TicketOperationType.CREATE,
                TicketOperationType.ASSIGN,
                TicketOperationType.PROCESS,
                TicketOperationType.CONFIRM,
                TicketOperationType.CANCEL);
    }

    @Test
    @DisplayName("每个操作类型都有中文标签")
    void shouldHaveChineseLabels() {
        assertThat(TicketOperationType.CREATE.getLabel()).isEqualTo("创建");
        assertThat(TicketOperationType.ASSIGN.getLabel()).isEqualTo("分派");
        assertThat(TicketOperationType.PROCESS.getLabel()).isEqualTo("处理");
        assertThat(TicketOperationType.CONFIRM.getLabel()).isEqualTo("确认");
        assertThat(TicketOperationType.CANCEL.getLabel()).isEqualTo("取消");
    }
}
