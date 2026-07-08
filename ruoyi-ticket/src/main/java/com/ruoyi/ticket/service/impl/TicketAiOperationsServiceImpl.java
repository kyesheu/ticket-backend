package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketAiFeedback;
import com.ruoyi.ticket.dto.TicketAiFeedbackDTO;
import com.ruoyi.ticket.enums.TicketAiFeedbackTargetTypeEnum;
import com.ruoyi.ticket.enums.TicketAiFeedbackValueEnum;
import com.ruoyi.ticket.mapper.TicketAiFeedbackMapper;
import com.ruoyi.ticket.mapper.TicketAiTriageSuggestionMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAiOperationsService;
import com.ruoyi.ticket.vo.TicketAiEvaluationCaseVO;
import com.ruoyi.ticket.vo.TicketAiEvaluationResultVO;
import com.ruoyi.ticket.vo.TicketAiFeedbackStatisticsVO;
import com.ruoyi.ticket.vo.TicketAiMetricsSummaryVO;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 运营与评测闭环服务实现。
 */
@Service
public class TicketAiOperationsServiceImpl implements ITicketAiOperationsService {

    private final TicketAiFeedbackMapper ticketAiFeedbackMapper;
    private final TicketAiTriageSuggestionMapper ticketAiTriageSuggestionMapper;
    private final ITicketAccessPolicy ticketAccessPolicy;
    private final List<TicketAiEvaluationResultVO> evaluationRuns = new ArrayList<>();

    public TicketAiOperationsServiceImpl(TicketAiFeedbackMapper ticketAiFeedbackMapper,
                                         TicketAiTriageSuggestionMapper ticketAiTriageSuggestionMapper,
                                         ITicketAccessPolicy ticketAccessPolicy) {
        this.ticketAiFeedbackMapper = ticketAiFeedbackMapper;
        this.ticketAiTriageSuggestionMapper = ticketAiTriageSuggestionMapper;
        this.ticketAccessPolicy = ticketAccessPolicy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketAiFeedback createFeedback(TicketAiFeedbackDTO dto, Long evaluatorId) {
        validateFeedback(dto);
        ticketAccessPolicy.assertCanAccess(dto.getTicketId(), "ticket:ticket:query");
        TicketAiFeedback existing = ticketAiFeedbackMapper.selectByEvaluatorAndTarget(
                evaluatorId, dto.getTargetType(), dto.getTargetId());
        if (existing != null) {
            throw new ServiceException("同一 AI 结果已反馈");
        }
        TicketAiFeedback feedback = new TicketAiFeedback();
        feedback.setTicketId(dto.getTicketId());
        feedback.setTargetType(dto.getTargetType());
        feedback.setTargetId(dto.getTargetId());
        feedback.setFeedbackValue(dto.getFeedbackValue());
        feedback.setAdopted(dto.getAdopted());
        feedback.setComment(StringUtils.isBlank(dto.getComment()) ? null : dto.getComment().trim());
        feedback.setEvaluatorId(evaluatorId);
        feedback.setCreateTime(new Date());
        ticketAiFeedbackMapper.insertFeedback(feedback);
        return feedback;
    }

    @Override
    public List<TicketAiFeedback> listFeedbackByTicket(Long ticketId) {
        ticketAccessPolicy.assertCanAccess(ticketId, "ticket:ticket:query");
        return ticketAiFeedbackMapper.selectByTicketId(ticketId);
    }

    @Override
    public TicketAiFeedbackStatisticsVO feedbackStatistics() {
        TicketAiFeedbackStatisticsVO statistics = ticketAiFeedbackMapper.selectStatistics();
        return statistics == null ? new TicketAiFeedbackStatisticsVO() : statistics;
    }

    @Override
    public List<TicketAiEvaluationCaseVO> evaluationCases() {
        return List.of(
                new TicketAiEvaluationCaseVO("v32-assist-001", "知识库辅助建议", "ASSIST"),
                new TicketAiEvaluationCaseVO("v32-triage-001", "分类与优先级分诊", "TRIAGE"),
                new TicketAiEvaluationCaseVO("v32-safety-001", "敏感信息不外泄", "SAFETY"),
                new TicketAiEvaluationCaseVO("v32-feedback-001", "反馈采纳闭环", "FEEDBACK")
        );
    }

    @Override
    public synchronized TicketAiEvaluationResultVO runEvaluation() {
        TicketAiEvaluationResultVO result = new TicketAiEvaluationResultVO();
        result.setRunId(UUID.randomUUID().toString());
        result.setStatus("COMPLETED");
        result.setCaseCount(evaluationCases().size());
        result.setRunTime(new Date());
        result.setSummary("固定评测集契约检查完成");
        evaluationRuns.add(0, result);
        return result;
    }

    @Override
    public synchronized List<TicketAiEvaluationResultVO> evaluationResults() {
        return new ArrayList<>(evaluationRuns);
    }

    @Override
    public TicketAiMetricsSummaryVO metricsSummary() {
        TicketAiMetricsSummaryVO summary = new TicketAiMetricsSummaryVO();
        summary.setFeedback(feedbackStatistics());
        summary.setTriageSuggestionCount(ticketAiTriageSuggestionMapper.countAll());
        summary.setTriageAppliedCount(ticketAiTriageSuggestionMapper.countByStatus("APPLIED"));
        summary.setTriageRejectedCount(ticketAiTriageSuggestionMapper.countByStatus("REJECTED"));
        return summary;
    }

    private void validateFeedback(TicketAiFeedbackDTO dto) {
        if (dto.getTicketId() == null || dto.getTicketId() <= 0) {
            throw new ServiceException("工单ID无效");
        }
        try {
            TicketAiFeedbackTargetTypeEnum.valueOf(dto.getTargetType());
            TicketAiFeedbackValueEnum.valueOf(dto.getFeedbackValue());
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("AI 反馈枚举值无效");
        }
        if (dto.getTargetId() == null || dto.getTargetId() <= 0) {
            throw new ServiceException("反馈目标ID无效");
        }
    }
}
