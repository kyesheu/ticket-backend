package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.ticket.domain.TicketAiTriageSuggestion;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.enums.TicketAiTriageSuggestionStatus;
import com.ruoyi.ticket.enums.TicketPriority;
import com.ruoyi.ticket.mapper.TicketAiTriageSuggestionMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.service.ITicketCategoryService;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketAiSourceVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import com.ruoyi.ticket.vo.TicketVO;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单 AI 分诊编排服务。
 */
@Service
public class TicketAiTriageServiceImpl implements ITicketAiTriageService {

    private static final String SOURCE_TYPE_KNOWLEDGE_DOCUMENT = "knowledge_document";
    private static final String SOURCE_TYPE_HISTORY_TICKET = "history_ticket";
    private static final int DEFAULT_TOP_K = 5;

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ITicketCategoryService ticketCategoryService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired(required = false)
    private ITicketAiService ticketAiService;

    @Autowired
    private TicketAiTriageSuggestionMapper suggestionMapper;

    @Autowired
    private TicketMapper ticketMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TicketAiTriageVO triage(Long ticketId) {
        TicketAiTriageRequestDTO request = buildRequest(ticketId);
        if (request.getAssigneeCandidates().isEmpty()) {
            return degraded("empty_assignee_candidates");
        }
        if (ticketAiService == null) {
            return degraded("ai_service_unavailable");
        }
        TicketAiTriageVO response = ticketAiService.triage(request);
        TicketAiTriageVO result = validateSuggestion(request, response);
        if (!Boolean.TRUE.equals(result.getDegraded())) {
            persistSuggestion(request, result);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long suggestionId, TicketAiTriageDecisionDTO dto) {
        TicketAiTriageSuggestion suggestion = getPendingSuggestion(suggestionId);
        TicketVO ticket = ticketService.selectTicketById(suggestion.getTicketId());
        if (!sameInstant(ticket.getUpdateTime(), suggestion.getTicketUpdatedAt())) {
            suggestionMapper.expirePending(suggestionId);
            throw new ServiceException("工单已变化，分诊建议已过期");
        }

        TicketAiTriageDecisionDTO finalDecision = fillDefaultDecision(suggestion, dto);
        TicketAiTriageRequestDTO request = buildRequest(suggestion.getTicketId());
        if (!decisionInCandidates(request, finalDecision)) {
            suggestionMapper.expirePending(suggestionId);
            throw new ServiceException("分诊候选已变化，建议已过期");
        }

        Date now = new Date();
        TicketAiTriageSuggestion update = new TicketAiTriageSuggestion();
        update.setSuggestionId(suggestionId);
        update.setFinalCategoryId(finalDecision.getCategoryId());
        update.setFinalPriority(finalDecision.getPriority());
        update.setFinalAssigneeId(finalDecision.getAssigneeId());
        update.setOperatedBy(SecurityUtils.getUserId());
        update.setOperatedAt(now);
        update.setUpdateTime(now);
        if (suggestionMapper.applyPending(update) == 0) {
            throw new ServiceException("分诊建议已处理");
        }

        ticketMapper.updateAiTriageFields(
                suggestion.getTicketId(),
                finalDecision.getCategoryId(),
                finalDecision.getPriority(),
                SecurityUtils.getUsername());

        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(finalDecision.getAssigneeId());
        assignDTO.setComment(StringUtils.isBlank(finalDecision.getComment()) ? "采纳 AI 分诊建议" : finalDecision.getComment());
        ticketService.assignTicket(suggestion.getTicketId(), assignDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long suggestionId) {
        TicketAiTriageSuggestion suggestion = getPendingSuggestion(suggestionId);
        if (suggestionMapper.rejectPending(suggestionId, SecurityUtils.getUserId()) == 0) {
            throw new ServiceException("分诊建议已处理");
        }
    }

    TicketAiTriageVO validateSuggestion(TicketAiTriageRequestDTO request, TicketAiTriageVO response) {
        if (Boolean.TRUE.equals(response.getDegraded())) {
            return response;
        }
        if (response.getConfidence() == null || response.getConfidence() < 0D || response.getConfidence() > 1D) {
            return degraded("invalid_confidence");
        }
        Set<Long> categoryIds = request.getCategoryCandidates().stream()
                .map(TicketAiTriageRequestDTO.CategoryCandidate::getCategoryId)
                .collect(Collectors.toSet());
        if (response.getSuggestedCategoryId() == null || !categoryIds.contains(response.getSuggestedCategoryId())) {
            return degraded("category_out_of_candidate_set");
        }
        Set<String> priorities = Set.copyOf(request.getPriorityCandidates());
        if (StringUtils.isBlank(response.getSuggestedPriority()) || !priorities.contains(response.getSuggestedPriority())) {
            return degraded("priority_out_of_candidate_set");
        }
        Set<Long> assigneeIds = request.getAssigneeCandidates().stream()
                .map(TicketAiTriageRequestDTO.AssigneeCandidate::getUserId)
                .collect(Collectors.toSet());
        if (response.getSuggestedAssigneeId() == null || !assigneeIds.contains(response.getSuggestedAssigneeId())) {
            return degraded("assignee_out_of_candidate_set");
        }
        if (!validSources(response)) {
            return degraded("forged_source_reference");
        }
        return response;
    }

    TicketAiTriageRequestDTO buildRequest(Long ticketId) {
        TicketVO ticket = ticketService.selectTicketById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        TicketAiTriageRequestDTO request = new TicketAiTriageRequestDTO();
        request.setTicketId(ticket.getTicketId());
        request.setTitle(ticket.getTitle());
        request.setDescription(ticket.getContent());
        request.setCurrentCategoryId(ticket.getCategoryId());
        request.setCurrentCategoryName(ticket.getCategoryName());
        request.setCurrentPriority(ticket.getPriority());
        request.setTicketUpdatedAt(toLocalDateTime(ticket.getUpdateTime()));
        request.setCategoryCandidates(buildCategoryCandidates());
        request.setPriorityCandidates(buildPriorityCandidates());
        request.setAssigneeCandidates(buildAssigneeCandidates(ticket));
        request.setTopK(DEFAULT_TOP_K);
        return request;
    }

    private List<TicketAiTriageRequestDTO.CategoryCandidate> buildCategoryCandidates() {
        TicketCategoryQueryDTO query = new TicketCategoryQueryDTO();
        query.setStatus(UserConstants.NORMAL);
        return ticketCategoryService.selectCategoryList(query).stream()
                .map(this::toCategoryCandidate)
                .toList();
    }

    private TicketAiTriageRequestDTO.CategoryCandidate toCategoryCandidate(TicketCategory category) {
        TicketAiTriageRequestDTO.CategoryCandidate candidate = new TicketAiTriageRequestDTO.CategoryCandidate();
        candidate.setCategoryId(category.getCategoryId());
        candidate.setCategoryName(category.getCategoryName());
        return candidate;
    }

    private List<String> buildPriorityCandidates() {
        return java.util.Arrays.stream(TicketPriority.values()).map(Enum::name).toList();
    }

    private List<TicketAiTriageRequestDTO.AssigneeCandidate> buildAssigneeCandidates(TicketVO ticket) {
        SysUser query = new SysUser();
        query.setStatus(UserConstants.NORMAL);
        query.setDeptId(ticket.getDeptId());
        List<SysUser> users = sysUserService.selectUserList(query);
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .filter(user -> UserConstants.NORMAL.equals(user.getStatus()))
                .filter(user -> user.getUserId() != null && StringUtils.isNotBlank(user.getUserName()))
                .map(this::toAssigneeCandidate)
                .toList();
    }

    private TicketAiTriageRequestDTO.AssigneeCandidate toAssigneeCandidate(SysUser user) {
        TicketAiTriageRequestDTO.AssigneeCandidate candidate = new TicketAiTriageRequestDTO.AssigneeCandidate();
        candidate.setUserId(user.getUserId());
        candidate.setUserName(user.getUserName());
        candidate.setNickName(user.getNickName());
        candidate.setDeptId(user.getDeptId());
        if (user.getDept() != null) {
            candidate.setDeptName(user.getDept().getDeptName());
        }
        return candidate;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        Date value = date == null ? new Date() : date;
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private boolean validSources(TicketAiTriageVO response) {
        if (response.getSources() == null || response.getSources().isEmpty()) {
            return false;
        }
        for (TicketAiSourceVO source : response.getSources()) {
            if (source == null || StringUtils.isBlank(source.getSourceId())) {
                return false;
            }
            if (!SOURCE_TYPE_KNOWLEDGE_DOCUMENT.equals(source.getSourceType())
                    && !SOURCE_TYPE_HISTORY_TICKET.equals(source.getSourceType())) {
                return false;
            }
        }
        return true;
    }

    private void persistSuggestion(TicketAiTriageRequestDTO request, TicketAiTriageVO response) {
        Date now = new Date();
        TicketAiTriageSuggestion suggestion = new TicketAiTriageSuggestion();
        suggestion.setTicketId(request.getTicketId());
        suggestion.setTicketUpdatedAt(Date.from(request.getTicketUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        suggestion.setSuggestedCategoryId(response.getSuggestedCategoryId());
        suggestion.setSuggestedPriority(response.getSuggestedPriority());
        suggestion.setSuggestedAssigneeId(response.getSuggestedAssigneeId());
        suggestion.setConfidence(BigDecimal.valueOf(response.getConfidence()));
        suggestion.setReasonSummary(response.getReasonSummary());
        suggestion.setSourceRefs(toSourceRefs(response));
        suggestion.setSuggestionStatus(TicketAiTriageSuggestionStatus.PENDING.name());
        suggestion.setCreateTime(now);
        suggestion.setUpdateTime(now);
        suggestionMapper.insertSuggestion(suggestion);
        response.setSuggestionId(suggestion.getSuggestionId());
    }

    private String toSourceRefs(TicketAiTriageVO response) {
        try {
            return objectMapper.writeValueAsString(response.getSources());
        } catch (JsonProcessingException exception) {
            throw new ServiceException("分诊来源序列化失败");
        }
    }

    private TicketAiTriageSuggestion getPendingSuggestion(Long suggestionId) {
        TicketAiTriageSuggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new ServiceException("分诊建议不存在");
        }
        if (!TicketAiTriageSuggestionStatus.PENDING.name().equals(suggestion.getSuggestionStatus())) {
            throw new ServiceException("分诊建议已处理");
        }
        return suggestion;
    }

    private TicketAiTriageDecisionDTO fillDefaultDecision(TicketAiTriageSuggestion suggestion,
                                                          TicketAiTriageDecisionDTO dto) {
        TicketAiTriageDecisionDTO source = dto == null ? new TicketAiTriageDecisionDTO() : dto;
        TicketAiTriageDecisionDTO result = new TicketAiTriageDecisionDTO();
        result.setCategoryId(source.getCategoryId() == null ? suggestion.getSuggestedCategoryId() : source.getCategoryId());
        result.setPriority(StringUtils.isBlank(source.getPriority()) ? suggestion.getSuggestedPriority() : source.getPriority());
        result.setAssigneeId(source.getAssigneeId() == null ? suggestion.getSuggestedAssigneeId() : source.getAssigneeId());
        result.setComment(source.getComment());
        return result;
    }

    private boolean decisionInCandidates(TicketAiTriageRequestDTO request, TicketAiTriageDecisionDTO decision) {
        boolean categoryAllowed = request.getCategoryCandidates().stream()
                .anyMatch(item -> Objects.equals(item.getCategoryId(), decision.getCategoryId()));
        boolean priorityAllowed = request.getPriorityCandidates().contains(decision.getPriority());
        boolean assigneeAllowed = request.getAssigneeCandidates().stream()
                .anyMatch(item -> Objects.equals(item.getUserId(), decision.getAssigneeId()));
        return categoryAllowed && priorityAllowed && assigneeAllowed;
    }

    private boolean sameInstant(Date current, Date snapshot) {
        if (current == null || snapshot == null) {
            return false;
        }
        return current.getTime() == snapshot.getTime();
    }

    private TicketAiTriageVO degraded(String reason) {
        TicketAiTriageVO result = new TicketAiTriageVO();
        result.setSources(Collections.emptyList());
        result.setDegraded(true);
        result.setReason(reason);
        result.setConfidence(0D);
        return result;
    }
}
