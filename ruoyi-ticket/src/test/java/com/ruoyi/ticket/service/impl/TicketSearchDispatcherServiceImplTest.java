package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchEvent;
import com.ruoyi.ticket.enums.TicketSearchEventStatus;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.service.ITicketSearchIndexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * 工单检索事件调度测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单检索事件调度测试")
class TicketSearchDispatcherServiceImplTest {

    @Mock private TicketSearchEventMapper eventMapper;
    @Mock private ITicketSearchIndexer searchIndexer;
    @InjectMocks private TicketSearchDispatcherServiceImpl dispatcherService;

    @Test
    @DisplayName("仅处理抢占成功的待处理事件")
    void shouldIndexOnlyClaimedEvent() {
        TicketSearchEvent event = event(21L, 9L, 0);
        when(eventMapper.selectDispatchableEvents(TicketSearchEventStatus.PENDING.name(), 50))
                .thenReturn(List.of(event));
        when(eventMapper.claimEvent(21L, TicketSearchEventStatus.PENDING.name(),
                TicketSearchEventStatus.PROCESSING.name())).thenReturn(1);

        dispatcherService.dispatchPendingEvents();

        verify(searchIndexer).upsertTicket(9L, 21L);
        verify(eventMapper).markSucceeded(21L, TicketSearchEventStatus.PROCESSING.name());
    }

    @Test
    @DisplayName("抢占失败时不处理事件")
    void shouldSkipEventWhenClaimFails() {
        TicketSearchEvent event = event(22L, 10L, 0);
        when(eventMapper.selectDispatchableEvents(TicketSearchEventStatus.PENDING.name(), 50))
                .thenReturn(List.of(event));

        dispatcherService.dispatchPendingEvents();

        verify(searchIndexer, never()).upsertTicket(10L, 22L);
    }

    @Test
    @DisplayName("索引失败时恢复为待处理并增加重试次数")
    void shouldRetryWhenIndexingFails() {
        TicketSearchEvent event = event(23L, 11L, 0);
        when(eventMapper.selectDispatchableEvents(TicketSearchEventStatus.PENDING.name(), 50))
                .thenReturn(List.of(event));
        when(eventMapper.claimEvent(23L, TicketSearchEventStatus.PENDING.name(),
                TicketSearchEventStatus.PROCESSING.name())).thenReturn(1);
        doThrow(new IllegalStateException("connection refused"))
                .when(searchIndexer).upsertTicket(11L, 23L);

        dispatcherService.dispatchPendingEvents();

        verify(eventMapper).markFailed(eq(23L), eq(TicketSearchEventStatus.PROCESSING.name()),
                eq(TicketSearchEventStatus.PENDING.name()), eq(1), any(),
                contains("工单索引写入失败"));
    }

    private TicketSearchEvent event(Long eventId, Long ticketId, int retryCount) {
        TicketSearchEvent event = new TicketSearchEvent();
        event.setEventId(eventId);
        event.setTicketId(ticketId);
        event.setRetryCount(retryCount);
        return event;
    }
}
