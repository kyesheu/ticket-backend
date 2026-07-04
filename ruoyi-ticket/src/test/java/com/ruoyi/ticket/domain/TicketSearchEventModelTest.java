package com.ruoyi.ticket.domain;

import com.ruoyi.ticket.enums.TicketSearchEventStatus;
import com.ruoyi.ticket.enums.TicketSearchEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单检索事件基础模型测试。
 */
@DisplayName("工单检索事件基础模型测试")
class TicketSearchEventModelTest {

    @Test
    @DisplayName("检索事件支持序列化并保存调度字段")
    void shouldMakeSearchEventSerializable() {
        TicketSearchEvent event = new TicketSearchEvent();
        event.setTicketId(10L);
        event.setRetryCount(2);

        assertThat(event).isInstanceOf(Serializable.class);
        assertThat(event.getTicketId()).isEqualTo(10L);
        assertThat(event.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("检索事件类型和值状态完整")
    void shouldContainSearchEventValues() {
        assertThat(TicketSearchEventType.values()).containsExactly(
                TicketSearchEventType.UPSERT,
                TicketSearchEventType.DELETE,
                TicketSearchEventType.REBUILD);
        assertThat(TicketSearchEventStatus.values()).containsExactly(
                TicketSearchEventStatus.PENDING,
                TicketSearchEventStatus.PROCESSING,
                TicketSearchEventStatus.SUCCEEDED,
                TicketSearchEventStatus.FAILED);
    }
}
