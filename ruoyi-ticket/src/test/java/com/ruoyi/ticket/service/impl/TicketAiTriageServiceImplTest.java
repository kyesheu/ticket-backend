package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.vo.TicketAiSourceVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单 AI 分诊服务测试。
 */
@DisplayName("工单 AI 分诊服务测试")
class TicketAiTriageServiceImplTest {

    private final TicketAiTriageServiceImpl service = new TicketAiTriageServiceImpl();

    @Test
    @DisplayName("候选集内分类和优先级通过校验")
    void shouldAcceptCategoryAndPriorityFromCandidates() {
        TicketAiTriageVO response = response(6L, "HIGH", 0.82D, source("knowledge_document", "doc-1"));

        TicketAiTriageVO result = service.validateSuggestion(request(), response);

        assertThat(result.getDegraded()).isFalse();
        assertThat(result.getSuggestedCategoryId()).isEqualTo(6L);
        assertThat(result.getSuggestedPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("候选集外分类被降级")
    void shouldDegradeCategoryOutsideCandidates() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(999L, "HIGH", 0.8D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("category_out_of_candidate_set");
        assertThat(result.getSuggestedCategoryId()).isNull();
    }

    @Test
    @DisplayName("候选集外优先级被降级")
    void shouldDegradePriorityOutsideCandidates() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "P0", 0.8D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("priority_out_of_candidate_set");
        assertThat(result.getSuggestedPriority()).isNull();
    }

    @Test
    @DisplayName("置信度越界被降级")
    void shouldDegradeInvalidConfidence() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "HIGH", 1.2D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("invalid_confidence");
    }

    @Test
    @DisplayName("伪造来源类型被降级")
    void shouldDegradeForgedSourceType() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "HIGH", 0.8D,
                source("system_prompt", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("forged_source_reference");
    }

    private TicketAiTriageRequestDTO request() {
        TicketAiTriageRequestDTO.CategoryCandidate category = new TicketAiTriageRequestDTO.CategoryCandidate();
        category.setCategoryId(6L);
        category.setCategoryName("网络故障");
        TicketAiTriageRequestDTO.AssigneeCandidate assignee = new TicketAiTriageRequestDTO.AssigneeCandidate();
        assignee.setUserId(1L);
        assignee.setUserName("admin");

        TicketAiTriageRequestDTO dto = new TicketAiTriageRequestDTO();
        dto.setTicketId(42L);
        dto.setTitle("WiFi 中断");
        dto.setDescription("办公室 WiFi 无法连接");
        dto.setTicketUpdatedAt(LocalDateTime.of(2026, 7, 6, 12, 0));
        dto.setCategoryCandidates(List.of(category));
        dto.setPriorityCandidates(List.of("LOW", "MEDIUM", "HIGH", "URGENT"));
        dto.setAssigneeCandidates(List.of(assignee));
        dto.setTopK(5);
        return dto;
    }

    private TicketAiTriageVO response(Long categoryId, String priority, Double confidence, TicketAiSourceVO source) {
        TicketAiTriageVO vo = new TicketAiTriageVO();
        vo.setSuggestedCategoryId(categoryId);
        vo.setSuggestedPriority(priority);
        vo.setConfidence(confidence);
        vo.setReasonSummary("证据匹配");
        vo.setSources(List.of(source));
        vo.setDegraded(false);
        return vo;
    }

    private TicketAiSourceVO source(String sourceType, String sourceId) {
        TicketAiSourceVO source = new TicketAiSourceVO();
        source.setSourceType(sourceType);
        source.setSourceId(sourceId);
        source.setTitle("网络故障处理");
        source.setSnippet("WiFi 故障");
        source.setScore(1.8D);
        return source;
    }
}
