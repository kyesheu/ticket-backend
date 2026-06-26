package com.ruoyi.ticket.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TicketStatus 状态流转单元测试
 *
 * @author ticket
 */
@DisplayName("工单状态流转测试")
class TicketStatusTest {

    @Test
    @DisplayName("NEW 可以分派到 PROCESSING")
    void newShouldAllowAssignToProcessing() {
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.PROCESSING)).isTrue();
    }

    @Test
    @DisplayName("NEW 可以取消到 CANCELLED")
    void newShouldAllowCancel() {
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("NEW 不能直接确认到 CLOSED")
    void newShouldNotAllowConfirm() {
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.CLOSED)).isFalse();
    }

    @Test
    @DisplayName("NEW 不能直接处理到 WAIT_CONFIRM")
    void newShouldNotAllowProcess() {
        assertThat(TicketStatus.NEW.canTransitionTo(TicketStatus.WAIT_CONFIRM)).isFalse();
    }

    @Test
    @DisplayName("PROCESSING 可以处理到 WAIT_CONFIRM")
    void processingShouldAllowProcess() {
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.WAIT_CONFIRM)).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 可以取消到 CANCELLED")
    void processingShouldAllowCancel() {
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 不能分派（已分派）")
    void processingShouldNotAllowAssign() {
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.PROCESSING)).isFalse();
        assertThat(TicketStatus.PROCESSING.canTransitionTo(TicketStatus.NEW)).isFalse();
    }

    @Test
    @DisplayName("WAIT_CONFIRM 可以确认到 CLOSED")
    void waitConfirmShouldAllowConfirm() {
        assertThat(TicketStatus.WAIT_CONFIRM.canTransitionTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("WAIT_CONFIRM 不能取消")
    void waitConfirmShouldNotAllowCancel() {
        assertThat(TicketStatus.WAIT_CONFIRM.canTransitionTo(TicketStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("WAIT_CONFIRM 不能分派")
    void waitConfirmShouldNotAllowAssign() {
        assertThat(TicketStatus.WAIT_CONFIRM.canTransitionTo(TicketStatus.PROCESSING)).isFalse();
    }

    @Test
    @DisplayName("CLOSED 终态不能做任何流转")
    void closedShouldNotAllowAnyTransition() {
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.NEW)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.PROCESSING)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.WAIT_CONFIRM)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.CANCELLED)).isFalse();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.CLOSED)).isFalse();
    }

    @Test
    @DisplayName("CANCELLED 终态不能做任何流转")
    void cancelledShouldNotAllowAnyTransition() {
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.NEW)).isFalse();
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.PROCESSING)).isFalse();
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.WAIT_CONFIRM)).isFalse();
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.CLOSED)).isFalse();
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("allowedTransitions 返回不可变集合")
    void allowedTransitionsShouldReturnUnmodifiableSet() {
        assertThat(TicketStatus.NEW.allowedTransitions()).containsExactlyInAnyOrder(
                TicketStatus.PROCESSING, TicketStatus.CANCELLED);
        assertThat(TicketStatus.PROCESSING.allowedTransitions()).containsExactlyInAnyOrder(
                TicketStatus.WAIT_CONFIRM, TicketStatus.CANCELLED);
        assertThat(TicketStatus.WAIT_CONFIRM.allowedTransitions()).containsExactlyInAnyOrder(
                TicketStatus.CLOSED);
        assertThat(TicketStatus.CLOSED.allowedTransitions()).isEmpty();
        assertThat(TicketStatus.CANCELLED.allowedTransitions()).isEmpty();
    }

    @Test
    @DisplayName("valueOf 可正确解析所有状态名")
    void valueOfShouldParseCorrectly() {
        assertThat(TicketStatus.valueOf("NEW")).isEqualTo(TicketStatus.NEW);
        assertThat(TicketStatus.valueOf("PROCESSING")).isEqualTo(TicketStatus.PROCESSING);
        assertThat(TicketStatus.valueOf("WAIT_CONFIRM")).isEqualTo(TicketStatus.WAIT_CONFIRM);
        assertThat(TicketStatus.valueOf("CLOSED")).isEqualTo(TicketStatus.CLOSED);
        assertThat(TicketStatus.valueOf("CANCELLED")).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("非法状态名抛出 IllegalArgumentException")
    void invalidStatusShouldThrow() {
        try {
            TicketStatus.valueOf("INVALID");
            org.assertj.core.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e).isNotNull();
        }
    }
}
