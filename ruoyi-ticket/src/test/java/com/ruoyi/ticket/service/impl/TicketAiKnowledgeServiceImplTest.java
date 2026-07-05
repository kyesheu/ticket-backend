package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiSyncCandidateService;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;
import com.ruoyi.ticket.vo.TicketVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 历史工单同步与相似知识检索 Service 测试。
 */
@DisplayName("历史工单同步与相似知识检索 Service 测试")
class TicketAiKnowledgeServiceImplTest {

    private ITicketAiSyncCandidateService candidateService;
    private ITicketAiService ticketAiService;
    private ITicketAccessPolicy accessPolicy;
    private TicketMapper ticketMapper;
    private TicketAiKnowledgeServiceImpl service;

    @BeforeEach
    void setUp() {
        candidateService = mock(ITicketAiSyncCandidateService.class);
        ticketAiService = mock(ITicketAiService.class);
        accessPolicy = mock(ITicketAccessPolicy.class);
        ticketMapper = mock(TicketMapper.class);
        service = new TicketAiKnowledgeServiceImpl(candidateService, ticketAiService, accessPolicy, ticketMapper);
    }

    @Test
    @DisplayName("仅同步候选查询返回的已关闭工单")
    void shouldSyncEligibleCandidates() {
        TicketAiClosedTicketSyncDTO dto = new TicketAiClosedTicketSyncDTO();
        when(candidateService.selectCandidatesAfter(0L, 100)).thenReturn(List.of(dto));

        assertThat(service.syncClosedTickets(0L, 100)).isEqualTo(1);

        verify(ticketAiService).syncClosedTicket(dto);
    }

    @Test
    @DisplayName("无对象权限时不查询工单且不调用 Python")
    void shouldNotCallPythonWithoutAccess() {
        doThrow(new ServiceException("工单不存在"))
                .when(accessPolicy).assertCanAccess(42L, "ticket:ticket:query");

        assertThatThrownBy(() -> service.searchSimilarKnowledge(42L))
                .isInstanceOf(ServiceException.class);

        verify(ticketMapper, never()).selectTicketById(42L);
        verify(ticketAiService, never()).search(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("有对象权限时使用当前工单标题和描述调用 Python")
    void shouldSearchAfterAccessCheck() {
        TicketVO ticket = new TicketVO();
        ticket.setTicketNo("TK202607050001");
        ticket.setTitle("Redis 缓存穿透");
        ticket.setContent("不存在的 key 被反复查询");
        ticket.setCategoryName("中间件");
        ticket.setPriority("HIGH");
        TicketAiSearchResultVO expected = new TicketAiSearchResultVO();
        when(ticketMapper.selectTicketById(42L)).thenReturn(ticket);
        when(ticketAiService.search(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        assertThat(service.searchSimilarKnowledge(42L)).isSameAs(expected);

        verify(accessPolicy).assertCanAccess(42L, "ticket:ticket:query");
        verify(ticketAiService).search(org.mockito.ArgumentMatchers.argThat(context ->
                "Redis 缓存穿透".equals(context.getTitle())
                        && "不存在的 key 被反复查询".equals(context.getDescription())));
    }
}
