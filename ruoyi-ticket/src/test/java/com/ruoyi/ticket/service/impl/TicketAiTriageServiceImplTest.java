package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketAiTriageSuggestion;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.enums.TicketAiTriageSuggestionStatus;
import com.ruoyi.ticket.mapper.TicketAiTriageSuggestionMapper;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketCategoryService;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketAiSourceVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import com.ruoyi.ticket.vo.TicketVO;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单 AI 分诊服务测试。
 */
@DisplayName("工单 AI 分诊服务测试")
@ExtendWith(MockitoExtension.class)
class TicketAiTriageServiceImplTest {

    @Mock
    private ITicketService ticketService;

    @Mock
    private ITicketCategoryService ticketCategoryService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private ITicketAiService ticketAiService;

    @Mock
    private TicketAiTriageSuggestionMapper suggestionMapper;

    @InjectMocks
    private TicketAiTriageServiceImpl service;

    @Test
    @DisplayName("候选集内分类和优先级通过校验")
    void shouldAcceptCategoryAndPriorityFromCandidates() {
        TicketAiTriageVO response = response(6L, "HIGH", 1L, 0.82D, source("knowledge_document", "doc-1"));

        TicketAiTriageVO result = service.validateSuggestion(request(), response);

        assertThat(result.getDegraded()).isFalse();
        assertThat(result.getSuggestedCategoryId()).isEqualTo(6L);
        assertThat(result.getSuggestedPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("候选集外分类被降级")
    void shouldDegradeCategoryOutsideCandidates() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(999L, "HIGH", 1L, 0.8D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("category_out_of_candidate_set");
        assertThat(result.getSuggestedCategoryId()).isNull();
    }

    @Test
    @DisplayName("候选集外优先级被降级")
    void shouldDegradePriorityOutsideCandidates() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "P0", 1L, 0.8D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("priority_out_of_candidate_set");
        assertThat(result.getSuggestedPriority()).isNull();
    }

    @Test
    @DisplayName("置信度越界被降级")
    void shouldDegradeInvalidConfidence() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "HIGH", 1L, 1.2D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("invalid_confidence");
    }

    @Test
    @DisplayName("伪造来源类型被降级")
    void shouldDegradeForgedSourceType() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "HIGH", 1L, 0.8D,
                source("system_prompt", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("forged_source_reference");
    }

    @Test
    @DisplayName("候选集外处理人被降级")
    void shouldDegradeAssigneeOutsideCandidates() {
        TicketAiTriageVO result = service.validateSuggestion(request(), response(6L, "HIGH", 999L, 0.8D,
                source("knowledge_document", "doc-1")));

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("assignee_out_of_candidate_set");
    }

    @Test
    @DisplayName("分诊请求只组装正常分类和工单部门正常用户")
    void shouldBuildRequestWithActiveCategoriesAndDeptAssignees() {
        when(ticketService.selectTicketById(42L)).thenReturn(ticket());
        when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                .thenReturn(List.of(category(6L, "网络故障")));
        when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of(user(1L, "admin", "0")));

        TicketAiTriageRequestDTO request = service.buildRequest(42L);

        assertThat(request.getCategoryCandidates()).singleElement()
                .satisfies(item -> assertThat(item.getCategoryId()).isEqualTo(6L));
        assertThat(request.getPriorityCandidates()).contains("LOW", "MEDIUM", "HIGH", "URGENT");
        assertThat(request.getAssigneeCandidates()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getUserId()).isEqualTo(1L);
                    assertThat(item.getDeptId()).isEqualTo(103L);
                });
    }

    @Test
    @DisplayName("空处理人候选直接降级且不调用 AI")
    void shouldDegradeEmptyAssigneeCandidatesBeforeCallingAi() {
        when(ticketService.selectTicketById(42L)).thenReturn(ticket());
        when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                .thenReturn(List.of(category(6L, "网络故障")));
        when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of());

        TicketAiTriageVO result = service.triage(42L);

        assertThat(result.getDegraded()).isTrue();
        assertThat(result.getReason()).isEqualTo("empty_assignee_candidates");
    }

    @Test
    @DisplayName("AI 返回结果必须经过 Java 候选白名单校验")
    void shouldValidateAiResponseAfterCallingService() {
        when(ticketService.selectTicketById(42L)).thenReturn(ticket());
        when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                .thenReturn(List.of(category(6L, "网络故障")));
        when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of(user(1L, "admin", "0")));
        when(ticketAiService.triage(any(TicketAiTriageRequestDTO.class)))
                .thenReturn(response(6L, "HIGH", 999L, 0.8D, source("knowledge_document", "doc-1")));

        TicketAiTriageVO result = service.triage(42L);

        verify(ticketAiService).triage(any(TicketAiTriageRequestDTO.class));
        assertThat(result.getReason()).isEqualTo("assignee_out_of_candidate_set");
    }

    @Test
    @DisplayName("生成有效分诊建议后应持久化并返回建议ID")
    void shouldPersistValidTriageSuggestion() {
        when(ticketService.selectTicketById(42L)).thenReturn(ticket());
        when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                .thenReturn(List.of(category(6L, "网络故障")));
        when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of(user(1L, "admin", "0")));
        when(ticketAiService.triage(any(TicketAiTriageRequestDTO.class)))
                .thenReturn(response(6L, "HIGH", 1L, 0.8D, source("knowledge_document", "doc-1")));
        when(suggestionMapper.insertSuggestion(any(TicketAiTriageSuggestion.class))).thenAnswer(invocation -> {
            invocation.<TicketAiTriageSuggestion>getArgument(0).setSuggestionId(100L);
            return 1;
        });

        TicketAiTriageVO result = service.triage(42L);

        assertThat(result.getSuggestionId()).isEqualTo(100L);
        verify(suggestionMapper).insertSuggestion(any(TicketAiTriageSuggestion.class));
    }

    @Test
    @DisplayName("采纳建议应保存最终选择并复用工单分派服务")
    void shouldApplySuggestionAndAssignTicket() {
        Date updatedAt = new Date();
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getUserId).thenReturn(9L);
            when(suggestionMapper.selectById(100L)).thenReturn(suggestion(updatedAt));
            when(ticketService.selectTicketById(42L)).thenReturn(ticket(updatedAt));
            when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                    .thenReturn(List.of(category(6L, "网络故障")));
            when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of(user(1L, "admin", "0")));
            when(suggestionMapper.applyPending(any(TicketAiTriageSuggestion.class))).thenReturn(1);

            service.apply(100L, null);
        }

        verify(suggestionMapper).applyPending(any(TicketAiTriageSuggestion.class));
        verify(ticketService).assignTicket(eq(42L), any(TicketAssignDTO.class));
    }

    @Test
    @DisplayName("修改后采纳应保存用户最终选择")
    void shouldApplyModifiedDecision() {
        Date updatedAt = new Date();
        TicketAiTriageDecisionDTO dto = new TicketAiTriageDecisionDTO();
        dto.setCategoryId(6L);
        dto.setPriority("URGENT");
        dto.setAssigneeId(2L);
        dto.setComment("人工调整后采纳");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getUserId).thenReturn(9L);
            when(suggestionMapper.selectById(100L)).thenReturn(suggestion(updatedAt));
            when(ticketService.selectTicketById(42L)).thenReturn(ticket(updatedAt));
            when(ticketCategoryService.selectCategoryList(any(TicketCategoryQueryDTO.class)))
                    .thenReturn(List.of(category(6L, "网络故障")));
            when(sysUserMapper.selectUserList(any(SysUser.class))).thenReturn(List.of(
                    user(1L, "admin", "0"), user(2L, "operator", "0")));
            when(suggestionMapper.applyPending(any(TicketAiTriageSuggestion.class))).thenReturn(1);

            service.apply(100L, dto);
        }

        verify(suggestionMapper).applyPending(org.mockito.ArgumentMatchers.argThat(update ->
                update.getFinalAssigneeId().equals(2L) && "URGENT".equals(update.getFinalPriority())));
    }

    @Test
    @DisplayName("工单更新时间变化时建议应过期且不分派")
    void shouldExpireWhenTicketChangedBeforeApply() {
        Date snapshot = new Date(1000L);
        Date changed = new Date(2000L);
        when(suggestionMapper.selectById(100L)).thenReturn(suggestion(snapshot));
        when(ticketService.selectTicketById(42L)).thenReturn(ticket(changed));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply(100L, null))
                .isInstanceOf(com.ruoyi.common.exception.ServiceException.class)
                .hasMessageContaining("已过期");

        verify(suggestionMapper).expirePending(100L);
        verify(ticketService, never()).assignTicket(any(), any());
    }

    @Test
    @DisplayName("已处理建议不能重复采纳")
    void shouldRejectRepeatedApply() {
        TicketAiTriageSuggestion suggestion = suggestion(new Date());
        suggestion.setSuggestionStatus(TicketAiTriageSuggestionStatus.APPLIED.name());
        when(suggestionMapper.selectById(100L)).thenReturn(suggestion);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply(100L, null))
                .isInstanceOf(com.ruoyi.common.exception.ServiceException.class)
                .hasMessageContaining("已处理");
    }

    @Test
    @DisplayName("拒绝建议应进入终态")
    void shouldRejectPendingSuggestion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getUserId).thenReturn(9L);
            when(suggestionMapper.selectById(100L)).thenReturn(suggestion(new Date()));
            when(suggestionMapper.rejectPending(100L, 9L)).thenReturn(1);

            service.reject(100L);
        }

        verify(suggestionMapper).rejectPending(100L, 9L);
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

    private TicketAiTriageVO response(Long categoryId, String priority, Long assigneeId,
                                      Double confidence, TicketAiSourceVO source) {
        TicketAiTriageVO vo = new TicketAiTriageVO();
        vo.setSuggestedCategoryId(categoryId);
        vo.setSuggestedPriority(priority);
        vo.setSuggestedAssigneeId(assigneeId);
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

    private TicketVO ticket() {
        return ticket(new Date());
    }

    private TicketVO ticket(Date updateTime) {
        TicketVO ticket = new TicketVO();
        ticket.setTicketId(42L);
        ticket.setTitle("WiFi 中断");
        ticket.setContent("办公室 WiFi 无法连接");
        ticket.setCategoryId(6L);
        ticket.setCategoryName("网络故障");
        ticket.setPriority("MEDIUM");
        ticket.setDeptId(103L);
        ticket.setUpdateTime(updateTime);
        return ticket;
    }

    private TicketCategory category(Long categoryId, String categoryName) {
        TicketCategory category = new TicketCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(categoryName);
        return category;
    }

    private SysUser user(Long userId, String userName, String status) {
        SysDept dept = new SysDept();
        dept.setDeptId(103L);
        dept.setDeptName("研发部门");
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickName("管理员");
        user.setStatus(status);
        user.setDeptId(103L);
        user.setDept(dept);
        return user;
    }

    private TicketAiTriageSuggestion suggestion(Date ticketUpdatedAt) {
        TicketAiTriageSuggestion suggestion = new TicketAiTriageSuggestion();
        suggestion.setSuggestionId(100L);
        suggestion.setTicketId(42L);
        suggestion.setTicketUpdatedAt(ticketUpdatedAt);
        suggestion.setSuggestedCategoryId(6L);
        suggestion.setSuggestedPriority("HIGH");
        suggestion.setSuggestedAssigneeId(1L);
        suggestion.setSuggestionStatus(TicketAiTriageSuggestionStatus.PENDING.name());
        return suggestion;
    }
}
