package com.ruoyi.ticket.service;

import com.ruoyi.ticket.domain.TicketAiFeedback;
import com.ruoyi.ticket.dto.TicketAiFeedbackDTO;
import com.ruoyi.ticket.vo.TicketAiEvaluationCaseVO;
import com.ruoyi.ticket.vo.TicketAiEvaluationResultVO;
import com.ruoyi.ticket.vo.TicketAiFeedbackStatisticsVO;
import com.ruoyi.ticket.vo.TicketAiMetricsSummaryVO;
import java.util.List;

/**
 * AI 运营与评测闭环服务。
 */
public interface ITicketAiOperationsService {

    TicketAiFeedback createFeedback(TicketAiFeedbackDTO dto, Long evaluatorId);

    List<TicketAiFeedback> listFeedbackByTicket(Long ticketId);

    TicketAiFeedbackStatisticsVO feedbackStatistics();

    List<TicketAiEvaluationCaseVO> evaluationCases();

    TicketAiEvaluationResultVO runEvaluation();

    List<TicketAiEvaluationResultVO> evaluationResults();

    TicketAiMetricsSummaryVO metricsSummary();
}
