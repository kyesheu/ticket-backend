package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchEvent;
import com.ruoyi.ticket.enums.TicketSearchEventStatus;
import com.ruoyi.ticket.enums.TicketSearchEventType;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 工单检索事件 Service 测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单检索事件 Service 测试")
class TicketSearchEventServiceImplTest {

    @Mock
    private TicketSearchEventMapper eventMapper;

    @InjectMocks
    private TicketSearchEventServiceImpl eventService;

    @Test
    @DisplayName("发布更新事件时保存可立即调度的待处理事件")
    void shouldPublishPendingUpsertEvent() {
        eventService.publishUpsert(12L);

        ArgumentCaptor<TicketSearchEvent> captor = ArgumentCaptor.forClass(TicketSearchEvent.class);
        verify(eventMapper).insertSearchEvent(captor.capture());
        TicketSearchEvent event = captor.getValue();
        assertThat(event.getTicketId()).isEqualTo(12L);
        assertThat(event.getEventType()).isEqualTo(TicketSearchEventType.UPSERT.name());
        assertThat(event.getEventStatus()).isEqualTo(TicketSearchEventStatus.PENDING.name());
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getCreateTime()).isNotNull();
    }
}
