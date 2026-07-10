package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketAiDispatchLog;
import com.ruoyi.ticket.domain.TicketAiEscalation;
import com.ruoyi.ticket.domain.TicketAiSession;
import com.ruoyi.ticket.domain.TicketAiSessionSource;
import com.ruoyi.ticket.dto.TicketAiAskDTO;
import com.ruoyi.ticket.dto.TicketAiEscalateDTO;
import com.ruoyi.ticket.dto.TicketAiQuestionAnswerRequestDTO;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.mapper.TicketAiDispatchMapper;
import com.ruoyi.ticket.mapper.TicketAiSessionMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketAiQuestionService;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketAiEscalateVO;
import com.ruoyi.ticket.vo.TicketAiQuestionAnswerVO;
import com.ruoyi.ticket.vo.TicketAiSourceVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 前置问答与转人工建单编排。
 */
@Service
public class TicketAiQuestionServiceImpl implements ITicketAiQuestionService {

    private static final double AUTO_ASSIGN_CONFIDENCE = 0.75D;
    private static final String SOURCE_TYPE_AI_ESCALATION = "AI_ESCALATION";
    private static final String DISPATCH_AUTO_ASSIGNED = "auto_assigned";
    private static final String DISPATCH_MANUAL_REQUIRED = "manual_required";
    private static final String DISPATCH_REJECTED = "rejected";
    private static final String DISPATCH_MODE_AI_AUTO = "AI_AUTO";
    private static final String DISPATCH_MODE_MANUAL = "MANUAL";

    @Autowired(required = false)
    private ITicketAiService ticketAiService;

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ITicketAiTriageService ticketAiTriageService;

    @Autowired
    private TicketAiSessionMapper ticketAiSessionMapper;

    @Autowired
    private TicketAiDispatchMapper ticketAiDispatchMapper;

    @Autowired
    private TicketMapper ticketMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TicketAiQuestionAnswerVO ask(TicketAiAskDTO dto) {
        TicketAiQuestionAnswerVO result;
        if (ticketAiService == null) {
            result = degraded("ai_service_unavailable");
            persistSession(dto.getQuestion(), result);
            return result;
        }
        TicketAiQuestionAnswerRequestDTO request = new TicketAiQuestionAnswerRequestDTO();
        request.setQuestion(dto.getQuestion());
        request.setCategory(dto.getCategory());
        request.setTopK(dto.getTopK() == null ? 5 : dto.getTopK());
        result = ticketAiService.ask(request);
        persistSession(dto.getQuestion(), result);
        return result;
    }

    @Override
    public void markResolved(Long sessionId) {
        if (sessionId == null || ticketAiSessionMapper.markResolved(sessionId, SecurityUtils.getUserId()) == 0) {
            throw new ServiceException("AI 问答会话不存在或已处理");
        }
    }

    @Override
    public TicketAiEscalateVO escalate(TicketAiEscalateDTO dto) {
        Long ticketId = ticketService.createTicket(buildTicket(dto));
        persistEscalation(dto, ticketId);
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
                updateDispatchResult(ticketId, DISPATCH_MODE_AI_AUTO, result.getDispatchReason());
                saveDispatchLog(dto.getSessionId(), ticketId, triage, DISPATCH_AUTO_ASSIGNED, result.getDispatchReason());
            } else {
                result.setDispatchReason(dispatchPendingReason(triage));
                updateDispatchResult(ticketId, DISPATCH_MODE_MANUAL, result.getDispatchReason());
                saveDispatchLog(dto.getSessionId(), ticketId, triage, DISPATCH_MANUAL_REQUIRED, result.getDispatchReason());
            }
        } catch (Exception exception) {
            result.setDispatchReason("已创建工单，自动分派失败：" + safeMessage(exception));
            updateDispatchResult(ticketId, DISPATCH_MODE_MANUAL, result.getDispatchReason());
            saveDispatchLog(dto.getSessionId(), ticketId, null, DISPATCH_REJECTED, result.getDispatchReason());
        }
        return result;
    }

    private void persistSession(String question, TicketAiQuestionAnswerVO result) {
        Date now = new Date();
        TicketAiSession session = new TicketAiSession();
        session.setUserId(SecurityUtils.getUserId());
        session.setQuestion(question);
        session.setAnswer(result.getAnswer());
        session.setSuggestion(result.getSuggestion());
        session.setConfidence(result.getConfidence() == null ? BigDecimal.ZERO : BigDecimal.valueOf(result.getConfidence()));
        session.setNeedHuman(Boolean.TRUE.equals(result.getNeedHuman()) ? "1" : "0");
        session.setDegraded(Boolean.TRUE.equals(result.getDegraded()) ? "1" : "0");
        session.setReason(result.getReason());
        session.setStatus("ACTIVE");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        ticketAiSessionMapper.insertSession(session);
        result.setSessionId(session.getSessionId());
        for (TicketAiSourceVO source : result.getSources() == null ? Collections.<TicketAiSourceVO>emptyList() : result.getSources()) {
            ticketAiSessionMapper.insertSource(toSessionSource(session.getSessionId(), source, now));
        }
    }

    private TicketAiSessionSource toSessionSource(Long sessionId, TicketAiSourceVO source, Date now) {
        TicketAiSessionSource row = new TicketAiSessionSource();
        row.setSessionId(sessionId);
        row.setSourceType(source.getSourceType());
        row.setSourceId(source.getSourceId());
        row.setTitle(source.getTitle());
        row.setSnippet(source.getSnippet());
        row.setScore(source.getScore() == null ? BigDecimal.ZERO : BigDecimal.valueOf(source.getScore()));
        row.setMetadataJson(toJson(source.getMetadata()));
        row.setCreateTime(now);
        return row;
    }

    private void persistEscalation(TicketAiEscalateDTO dto, Long ticketId) {
        Long userId = SecurityUtils.getUserId();
        if (dto.getSessionId() != null) {
            ticketAiSessionMapper.markEscalated(dto.getSessionId(), userId, ticketId);
        }
        TicketAiEscalation escalation = new TicketAiEscalation();
        escalation.setSessionId(dto.getSessionId());
        escalation.setTicketId(ticketId);
        escalation.setUserId(userId);
        escalation.setUserComment(dto.getUserComment());
        escalation.setAiSummary(summary(dto));
        escalation.setCreateTime(new Date());
        ticketAiSessionMapper.insertEscalation(escalation);
    }

    private TicketCreateDTO buildTicket(TicketAiEscalateDTO dto) {
        TicketCreateDTO ticket = new TicketCreateDTO();
        ticket.setTitle(buildTitle(dto.getQuestion()));
        ticket.setContent(buildContent(dto));
        ticket.setCategoryId(dto.getCategoryId());
        ticket.setPriority(StringUtils.isBlank(dto.getPriority()) ? "MEDIUM" : dto.getPriority());
        ticket.setAttachmentIds(dto.getAttachmentIds());
        ticket.setSourceType(SOURCE_TYPE_AI_ESCALATION);
        ticket.setAiSessionId(dto.getSessionId());
        ticket.setAiSummary(summary(dto));
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
                && triage.getConfidence() >= AUTO_ASSIGN_CONFIDENCE
                && ticketAiDispatchMapper.countEnabledRule(
                        triage.getSuggestedCategoryId(),
                        triage.getSuggestedAssigneeId(),
                        triage.getSuggestedPriority()) > 0;
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

    private void updateDispatchResult(Long ticketId, String dispatchMode, String dispatchReason) {
        ticketMapper.updateAiDispatchResult(ticketId, dispatchMode, truncate(dispatchReason, 1000), SecurityUtils.getUsername());
    }

    private void saveDispatchLog(Long sessionId, Long ticketId, TicketAiTriageVO triage, String decision, String reason) {
        TicketAiDispatchLog log = new TicketAiDispatchLog();
        log.setTicketId(ticketId);
        log.setSessionId(sessionId);
        if (triage != null) {
            log.setSuggestedCategoryId(triage.getSuggestedCategoryId());
            log.setSuggestedPriority(triage.getSuggestedPriority());
            log.setSuggestedAssigneeId(triage.getSuggestedAssigneeId());
            log.setConfidence(triage.getConfidence() == null ? BigDecimal.ZERO : BigDecimal.valueOf(triage.getConfidence()));
        } else {
            log.setConfidence(BigDecimal.ZERO);
        }
        log.setDecision(decision);
        log.setReason(truncate(reason, 1000));
        log.setCreateTime(new Date());
        ticketAiDispatchMapper.insertLog(log);
    }

    private String summary(TicketAiEscalateDTO dto) {
        String answer = StringUtils.isBlank(dto.getAiAnswer()) ? "" : dto.getAiAnswer();
        return answer.length() > 1000 ? answer.substring(0, 1000) : answer;
    }

    private String truncate(String text, int length) {
        if (text == null || text.length() <= length) {
            return text;
        }
        return text.substring(0, length);
    }

    private String toJson(Object value) {
        try {
            return value == null ? "{}" : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private TicketAiQuestionAnswerVO degraded(String reason) {
        TicketAiQuestionAnswerVO result = new TicketAiQuestionAnswerVO();
        result.setAnswer("AI 服务暂时不可用，请直接转人工处理。");
        result.setSuggestion("");
        result.setConfidence(0D);
        result.setNeedHuman(true);
        result.setSources(Collections.emptyList());
        result.setDegraded(true);
        result.setReason(reason);
        return result;
    }

    private String safeMessage(Exception exception) {
        return StringUtils.isBlank(exception.getMessage()) ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
