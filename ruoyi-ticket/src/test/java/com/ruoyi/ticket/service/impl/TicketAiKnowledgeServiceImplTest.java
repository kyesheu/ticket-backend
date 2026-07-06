package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.exception.TicketAiServiceException;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiSyncCandidateService;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
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

    @Test
    @DisplayName("有对象权限时组装当前工单上下文并返回可编辑草稿")
    void shouldAssistAfterAccessCheckWithoutPersistingDraft() {
        TicketVO ticket = new TicketVO();
        ticket.setTitle("Redis 缓存穿透");
        ticket.setContent("不存在的 key 被反复查询");
        ticket.setCategoryName("中间件");
        TicketAiAssistVO expected = new TicketAiAssistVO();
        expected.setDegraded(true);
        expected.setReason("model_timeout");
        when(ticketMapper.selectTicketById(42L)).thenReturn(ticket);
        when(ticketAiService.assist(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        assertThat(service.assist(42L, 5)).isSameAs(expected);

        verify(accessPolicy).assertCanAccess(42L, "ticket:ticket:query");
        verify(ticketAiService).assist(org.mockito.ArgumentMatchers.argThat(request ->
                request.getTicketId().equals(42L)
                        && request.getTopK().equals(5)
                        && "Redis 缓存穿透".equals(request.getTitle())));
    }

    @Test
    @DisplayName("工单辅助无对象权限时不查询工单且不调用 Python")
    void shouldNotAssistWithoutAccess() {
        doThrow(new ServiceException("工单不存在"))
                .when(accessPolicy).assertCanAccess(42L, "ticket:ticket:query");

        assertThatThrownBy(() -> service.assist(42L, 5)).isInstanceOf(ServiceException.class);

        verify(ticketMapper, never()).selectTicketById(42L);
        verify(ticketAiService, never()).assist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Python 不可用时返回降级结果且不影响工单主流程")
    void shouldDegradeWhenPythonIsUnavailable() {
        TicketVO ticket = new TicketVO();
        ticket.setTitle("Redis 缓存穿透");
        ticket.setContent("问题描述");
        when(ticketMapper.selectTicketById(42L)).thenReturn(ticket);
        when(ticketAiService.assist(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new TicketAiServiceException("AI 服务调用失败"));

        TicketAiAssistVO result = service.assist(42L, 5);

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("python_service_unavailable");
        assertThat(result.getSuggestion()).isEmpty();
        assertThat(result.getReplyDraft()).isEmpty();
        verify(accessPolicy).assertCanAccess(42L, "ticket:ticket:query");
    }
}
