package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchRebuild;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.mapper.TicketSearchRebuildMapper;
import com.ruoyi.ticket.service.ITicketSearchIndexer;
import com.ruoyi.ticket.service.TicketSearchIndexAdminGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单检索全量重建 Worker 测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单检索全量重建 Worker 测试")
class TicketSearchRebuildWorkerTest {
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketSearchRebuildMapper rebuildMapper;
    @Mock private TicketSearchEventMapper eventMapper;
    @Mock private ITicketSearchIndexer searchIndexer;
    @Mock private TicketSearchIndexAdminGateway indexAdminGateway;
    @InjectMocks private TicketSearchRebuildWorker worker;

    @Test
    @DisplayName("数量校验通过后切换别名并重放增量事件")
    void shouldSwitchAliasAfterSuccessfulRebuild() {
        TicketSearchRebuild rebuild = rebuild();
        when(ticketMapper.selectSearchableTicketIdsAfter(0L, 5L, 200)).thenReturn(List.of(3L, 5L));
        when(ticketMapper.selectSearchableTicketIdsAfter(5L, 5L, 200)).thenReturn(List.of());
        when(indexAdminGateway.countDocuments("ticket-search-v1")).thenReturn(2L);

        worker.rebuild(rebuild);

        verify(searchIndexer).indexTicketTo(3L, 10L, "ticket-search-v1");
        verify(searchIndexer).indexTicketTo(5L, 10L, "ticket-search-v1");
        verify(indexAdminGateway).refreshIndex("ticket-search-v1");
        verify(indexAdminGateway).switchAlias("ticket-search-v1");
        verify(eventMapper).requeueEventsAfter(10L);
        verify(rebuildMapper).markSucceeded(2L);
    }

    @Test
    @DisplayName("重建失败时保留线上别名并记录脱敏失败状态")
    void shouldKeepAliasWhenRebuildFails() {
        TicketSearchRebuild rebuild = rebuild();
        doThrow(new IllegalStateException("secret endpoint"))
                .when(indexAdminGateway).createIndex("ticket-search-v1");

        worker.rebuild(rebuild);

        verify(indexAdminGateway, never()).switchAlias("ticket-search-v1");
        verify(rebuildMapper).markFailed(contains("工单索引重建失败"));
    }

    private TicketSearchRebuild rebuild() {
        TicketSearchRebuild rebuild = new TicketSearchRebuild();
        rebuild.setIndexName("ticket-search-v1");
        rebuild.setTotalCount(2L);
        rebuild.setStartEventId(10L);
        rebuild.setMaxTicketId(5L);
        return rebuild;
    }
}
