package com.ruoyi.ticket.controller;

import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiKnowledgeService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单 AI Controller 测试。
 */
@DisplayName("工单 AI Controller 测试")
class TicketAiControllerTest {

    @Test
    @DisplayName("辅助接口只返回 Service 结果不触发工单写操作")
    void shouldOnlyReturnEditableAssistResult() {
        ITicketAiDocumentService documentService = mock(ITicketAiDocumentService.class);
        ITicketAiKnowledgeService knowledgeService = mock(ITicketAiKnowledgeService.class);
        ITicketAiTriageService triageService = mock(ITicketAiTriageService.class);
        TicketAiAssistVO response = new TicketAiAssistVO();
        response.setDegraded(true);
        response.setReason("no_reliable_evidence");
        when(knowledgeService.assist(42L, 5)).thenReturn(response);
        TicketAiController controller = new TicketAiController(documentService, knowledgeService, triageService);

        controller.assist(42L, 5);

        verify(knowledgeService).assist(42L, 5);
    }

    @Test
    @DisplayName("分诊接口只返回 Service 结果不触发工单写操作")
    void shouldOnlyReturnTriageSuggestion() {
        ITicketAiDocumentService documentService = mock(ITicketAiDocumentService.class);
        ITicketAiKnowledgeService knowledgeService = mock(ITicketAiKnowledgeService.class);
        ITicketAiTriageService triageService = mock(ITicketAiTriageService.class);
        TicketAiTriageVO response = new TicketAiTriageVO();
        response.setDegraded(true);
        response.setReason("stage49_contract_only");
        when(triageService.triage(42L)).thenReturn(response);
        TicketAiController controller = new TicketAiController(documentService, knowledgeService, triageService);

        controller.triage(42L);

        verify(triageService).triage(42L);
    }
}
