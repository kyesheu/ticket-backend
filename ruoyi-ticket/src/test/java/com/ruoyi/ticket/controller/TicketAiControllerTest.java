package com.ruoyi.ticket.controller;

import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiKnowledgeService;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
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
        TicketAiAssistVO response = new TicketAiAssistVO();
        response.setDegraded(true);
        response.setReason("no_reliable_evidence");
        when(knowledgeService.assist(42L, 5)).thenReturn(response);
        TicketAiController controller = new TicketAiController(documentService, knowledgeService);

        controller.assist(42L, 5);

        verify(knowledgeService).assist(42L, 5);
    }
}
