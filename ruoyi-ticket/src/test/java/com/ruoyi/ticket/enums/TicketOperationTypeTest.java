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
    @DisplayName("枚举包含全部操作类型")
    void shouldContainAllValues() {
        TicketOperationType[] values = TicketOperationType.values();
        assertThat(values).hasSize(7);
        assertThat(values).containsExactly(
                TicketOperationType.CREATE,
                TicketOperationType.ASSIGN,
                TicketOperationType.PROCESS,
                TicketOperationType.CONFIRM,
                TicketOperationType.CANCEL,
                TicketOperationType.RETURN,
                TicketOperationType.TERMINATE);
    }

    @Test
    @DisplayName("每个操作类型都有中文标签")
    void shouldHaveChineseLabels() {
        assertThat(TicketOperationType.CREATE.getLabel()).isEqualTo("创建");
        assertThat(TicketOperationType.ASSIGN.getLabel()).isEqualTo("分派");
        assertThat(TicketOperationType.PROCESS.getLabel()).isEqualTo("处理");
        assertThat(TicketOperationType.CONFIRM.getLabel()).isEqualTo("确认");
        assertThat(TicketOperationType.CANCEL.getLabel()).isEqualTo("取消");
        assertThat(TicketOperationType.RETURN.getLabel()).isEqualTo("退回");
        assertThat(TicketOperationType.TERMINATE.getLabel()).isEqualTo("终止");
    }
}
