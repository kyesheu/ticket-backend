package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAiSyncCandidate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 历史工单同步候选查询 Service 测试。
 */
@DisplayName("历史工单同步候选查询 Service 测试")
class TicketAiSyncCandidateServiceImplTest {

    private TicketMapper ticketMapper;
    private TicketAiSyncCandidateServiceImpl service;

    @BeforeEach
    void setUp() {
        ticketMapper = mock(TicketMapper.class);
        service = new TicketAiSyncCandidateServiceImpl(ticketMapper, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("查询 CLOSED 且有解决方案的工单并组装同步 DTO")
    void shouldMapEligibleClosedTicket() {
        TicketAiSyncCandidate candidate = candidate("CLOSED", "  使用空值缓存  ", 15L);
        when(ticketMapper.selectAiSyncCandidatesAfter(10L, 20)).thenReturn(List.of(candidate));

        List<TicketAiClosedTicketSyncDTO> result = service.selectCandidatesAfter(10L, 20);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getTicketId()).isEqualTo(11L);
            assertThat(dto.getCategory()).isEqualTo("中间件");
            assertThat(dto.getDescription()).isEqualTo("Redis 缓存穿透");
            assertThat(dto.getSolution()).isEqualTo("使用空值缓存");
            assertThat(dto.getStatus()).isEqualTo("CLOSED");
            assertThat(dto.getTags()).containsExactly("中间件", "HIGH");
            assertThat(dto.getCreatedTime()).isEqualTo("2026-07-01T01:00:00Z");
            assertThat(dto.getClosedTime()).isEqualTo("2026-07-01T02:00:00Z");
            assertThat(dto.getSourceGeneration()).isEqualTo(15L);
        });
        verify(ticketMapper).selectAiSyncCandidatesAfter(10L, 20);
    }

    @Test
    @DisplayName("Service 再次过滤非 CLOSED、空解决方案和缺少代次的结果")
    void shouldDefensivelyFilterIneligibleCandidates() {
        when(ticketMapper.selectAiSyncCandidatesAfter(0L, 10)).thenReturn(List.of(
                candidate("PROCESSING", "处理中", 1L),
                candidate("CLOSED", " ", 2L),
                candidate("CLOSED", "已解决", null)));

        assertThat(service.selectCandidatesAfter(0L, 10)).isEmpty();
    }

    @Test
    @DisplayName("非法游标和批量大小在查询前被拒绝")
    void shouldRejectInvalidPagingArguments() {
        assertThatThrownBy(() -> service.selectCandidatesAfter(-1L, 10))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.selectCandidatesAfter(0L, 101))
                .isInstanceOf(ServiceException.class);
    }

    private TicketAiSyncCandidate candidate(String status, String solution, Long generation) {
        TicketAiSyncCandidate candidate = new TicketAiSyncCandidate();
        candidate.setTicketId(11L);
        candidate.setTitle("Redis 问题");
        candidate.setCategory("中间件");
        candidate.setDescription("Redis 缓存穿透");
        candidate.setSolution(solution);
        candidate.setStatus(status);
        candidate.setPriority("HIGH");
        candidate.setCreatedTime(Date.from(Instant.parse("2026-07-01T01:00:00Z")));
        candidate.setClosedTime(Date.from(Instant.parse("2026-07-01T02:00:00Z")));
        candidate.setSourceGeneration(generation);
        return candidate;
    }
}
