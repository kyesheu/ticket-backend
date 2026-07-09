package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.dto.TicketAiAskDTO;
import com.ruoyi.ticket.dto.TicketAiEscalateDTO;
import com.ruoyi.ticket.dto.TicketAiQuestionAnswerRequestDTO;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.service.ITicketAiQuestionService;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketAiEscalateVO;
import com.ruoyi.ticket.vo.TicketAiQuestionAnswerVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 前置问答与转人工建单编排。
 */
@Service
public class TicketAiQuestionServiceImpl implements ITicketAiQuestionService {

    private static final double AUTO_ASSIGN_CONFIDENCE = 0.75D;

    @Autowired(required = false)
    private ITicketAiService ticketAiService;

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ITicketAiTriageService ticketAiTriageService;

    @Override
    public TicketAiQuestionAnswerVO ask(TicketAiAskDTO dto) {
        if (ticketAiService == null) {
            return degraded("ai_service_unavailable");
        }
        TicketAiQuestionAnswerRequestDTO request = new TicketAiQuestionAnswerRequestDTO();
        request.setQuestion(dto.getQuestion());
        request.setCategory(dto.getCategory());
        request.setTopK(dto.getTopK() == null ? 5 : dto.getTopK());
        return ticketAiService.ask(request);
    }

    @Override
    public TicketAiEscalateVO escalate(TicketAiEscalateDTO dto) {
        Long ticketId = ticketService.createTicket(buildTicket(dto));
        TicketAiEscalateVO result = new TicketAiEscalateVO();
        result.setTicketId(ticketId);
        result.setAutoAssigned(false);

        try {
            TicketAiTriageVO triage = ticketAiTriageService.triage(ticketId);
            result.setTriage(triage);
            if (canAutoAssign(triage)) {
                TicketAiTriageDecisionDTO decision = new TicketAiTriageDecisionDTO();
                decision.setCategoryId(triage.getSuggestedCategoryId());
                decision.setPriority(triage.getSuggestedPriority());
                decision.setAssigneeId(triage.getSuggestedAssigneeId());
                decision.setComment("AI 自动分派，置信度 " + triage.getConfidence());
                ticketAiTriageService.apply(triage.getSuggestionId(), decision);
                result.setAutoAssigned(true);
                result.setDispatchReason("AI 自动分派成功");
            } else {
                result.setDispatchReason(dispatchPendingReason(triage));
            }
        } catch (Exception exception) {
            result.setDispatchReason("已创建工单，自动分派失败：" + safeMessage(exception));
        }
        return result;
    }

    private TicketCreateDTO buildTicket(TicketAiEscalateDTO dto) {
        TicketCreateDTO ticket = new TicketCreateDTO();
        ticket.setTitle(buildTitle(dto.getQuestion()));
        ticket.setContent(buildContent(dto));
        ticket.setCategoryId(dto.getCategoryId());
        ticket.setPriority(StringUtils.isBlank(dto.getPriority()) ? "MEDIUM" : dto.getPriority());
        ticket.setAttachmentIds(dto.getAttachmentIds());
        return ticket;
    }

    private String buildTitle(String question) {
        String firstLine = question == null ? "" : question.strip().split("\\R", 2)[0];
        if (StringUtils.isBlank(firstLine)) {
            throw new ServiceException("问题不能为空");
        }
        return firstLine.length() > 80 ? firstLine.substring(0, 80) : firstLine;
    }

    private String buildContent(TicketAiEscalateDTO dto) {
        StringBuilder content = new StringBuilder();
        content.append("【用户原始问题】\n").append(dto.getQuestion()).append("\n\n");
        if (StringUtils.isNotBlank(dto.getAiAnswer())) {
            content.append("【AI 初始回答】\n").append(dto.getAiAnswer()).append("\n\n");
        }
        if (StringUtils.isNotBlank(dto.getAiSuggestion())) {
            content.append("【AI 处理建议】\n").append(dto.getAiSuggestion()).append("\n\n");
        }
        if (StringUtils.isNotBlank(dto.getUserComment())) {
            content.append("【用户补充说明】\n").append(dto.getUserComment()).append("\n\n");
        }
        content.append("【来源】AI 智能问答转人工");
        return content.toString();
    }

    private boolean canAutoAssign(TicketAiTriageVO triage) {
        return triage != null
                && !Boolean.TRUE.equals(triage.getDegraded())
                && triage.getSuggestionId() != null
                && triage.getSuggestedAssigneeId() != null
                && triage.getConfidence() != null
                && triage.getConfidence() >= AUTO_ASSIGN_CONFIDENCE;
    }

    private String dispatchPendingReason(TicketAiTriageVO triage) {
        if (triage == null) {
            return "已创建工单，未生成 AI 分派建议";
        }
        if (Boolean.TRUE.equals(triage.getDegraded())) {
            return "已创建工单，AI 分派降级：" + triage.getReason();
        }
        return "已创建工单，AI 分派置信度不足或缺少处理人";
    }

    private TicketAiQuestionAnswerVO degraded(String reason) {
        TicketAiQuestionAnswerVO result = new TicketAiQuestionAnswerVO();
        result.setAnswer("AI 服务暂时不可用，请直接转人工处理。");
        result.setSuggestion("");
        result.setConfidence(0D);
        result.setNeedHuman(true);
        result.setDegraded(true);
        result.setReason(reason);
        return result;
    }

    private String safeMessage(Exception exception) {
        return StringUtils.isBlank(exception.getMessage()) ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
