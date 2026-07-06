package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.vo.TicketAiSourceVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 工单 AI 分诊编排服务。
 */
@Service
public class TicketAiTriageServiceImpl implements ITicketAiTriageService {

    private static final String SOURCE_TYPE_KNOWLEDGE_DOCUMENT = "knowledge_document";
    private static final String SOURCE_TYPE_HISTORY_TICKET = "history_ticket";

    @Override
    public TicketAiTriageVO triage(Long ticketId) {
        // TODO: 阶段51实现真实候选集组装、AI调用和处理人排序
        TicketAiTriageVO result = new TicketAiTriageVO();
        result.setSources(Collections.emptyList());
        result.setDegraded(true);
        result.setReason("stage50_contract_only");
        result.setConfidence(0D);
        return result;
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
        if (!validSources(response)) {
            return degraded("forged_source_reference");
        }
        return response;
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

    private TicketAiTriageVO degraded(String reason) {
        TicketAiTriageVO result = new TicketAiTriageVO();
        result.setSources(Collections.emptyList());
        result.setDegraded(true);
        result.setReason(reason);
        result.setConfidence(0D);
        return result;
    }
}
