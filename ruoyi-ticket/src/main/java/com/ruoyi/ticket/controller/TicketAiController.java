package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketAiFeedbackDTO;
import com.ruoyi.ticket.dto.TicketAiAskDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.dto.TicketAiEscalateDTO;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiKnowledgeService;
import com.ruoyi.ticket.service.ITicketAiOperationsService;
import com.ruoyi.ticket.service.ITicketAiQuestionService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 工单 AI 管理接口。
 */
@Tag(name = "工单 AI", description = "知识文档导入与工单智能辅助")
@RestController
@RequestMapping("/ticket/ai")
@ConditionalOnProperty(prefix = "ticket.ai", name = "enabled", havingValue = "true")
public class TicketAiController extends BaseController {

    private final ITicketAiDocumentService ticketAiDocumentService;
    private final ITicketAiKnowledgeService ticketAiKnowledgeService;
    private final ITicketAiTriageService ticketAiTriageService;
    private final ITicketAiOperationsService ticketAiOperationsService;
    private final ITicketAiQuestionService ticketAiQuestionService;

    public TicketAiController(ITicketAiDocumentService ticketAiDocumentService,
                              ITicketAiKnowledgeService ticketAiKnowledgeService,
                              ITicketAiTriageService ticketAiTriageService,
                              ITicketAiOperationsService ticketAiOperationsService,
                              ITicketAiQuestionService ticketAiQuestionService) {
        this.ticketAiDocumentService = ticketAiDocumentService;
        this.ticketAiKnowledgeService = ticketAiKnowledgeService;
        this.ticketAiTriageService = ticketAiTriageService;
        this.ticketAiOperationsService = ticketAiOperationsService;
        this.ticketAiQuestionService = ticketAiQuestionService;
    }

    @Operation(summary = "AI 智能问答")
    @PreAuthorize("@ss.hasPermi('ticket:ai:ask')")
    @PostMapping("/ask")
    public AjaxResult ask(@Valid @RequestBody TicketAiAskDTO dto) {
        return success(ticketAiQuestionService.ask(dto));
    }

    @Operation(summary = "标记 AI 问答已解决")
    @PreAuthorize("@ss.hasPermi('ticket:ai:ask')")
    @PostMapping("/session/{sessionId}/resolved")
    public AjaxResult markResolved(@PathVariable Long sessionId) {
        ticketAiQuestionService.markResolved(sessionId);
        return success();
    }

    @Operation(summary = "AI 问答转人工建单")
    @Log(title = "AI 转人工建单", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:escalate')")
    @PostMapping("/escalate")
    public AjaxResult escalate(@Valid @RequestBody TicketAiEscalateDTO dto) {
        return success(ticketAiQuestionService.escalate(dto));
    }

    @Operation(summary = "导入知识文档")
    @Log(title = "AI 知识文档", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:import')")
    @PostMapping("/document/import")
    public AjaxResult importDocument(@RequestParam String sourceId,
                                     @RequestParam(required = false) String categoryName,
                                     @RequestParam("file") MultipartFile file) {
        return success(ticketAiDocumentService.importDocument(sourceId, categoryName, file));
    }

    @Operation(summary = "分页查询知识文档")
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:list')")
    @GetMapping("/documents")
    public AjaxResult listDocuments(TicketAiDocumentQueryDTO query) {
        return success(ticketAiDocumentService.listDocuments(query));
    }

    @Operation(summary = "查询知识文档详情")
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:query')")
    @GetMapping("/documents/{sourceId}")
    public AjaxResult getDocument(@PathVariable String sourceId) {
        return success(ticketAiDocumentService.getDocument(sourceId));
    }

    @Operation(summary = "重导知识文档")
    @Log(title = "AI 知识文档重导", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:edit')")
    @PutMapping("/documents/{sourceId}/reimport")
    public AjaxResult reimportDocument(@PathVariable String sourceId) {
        return success(ticketAiDocumentService.reimportDocument(sourceId));
    }

    @Operation(summary = "删除知识文档")
    @Log(title = "AI 知识文档删除", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:remove')")
    @DeleteMapping("/documents/{sourceId}")
    public AjaxResult deleteDocument(@PathVariable String sourceId) {
        return success(ticketAiDocumentService.deleteDocument(sourceId));
    }

    @Operation(summary = "同步历史已关闭工单")
    @Log(title = "AI 历史工单同步", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:history:sync')")
    @PostMapping("/history/sync")
    public AjaxResult syncHistory(@RequestParam(defaultValue = "0") Long lastTicketId,
                                  @RequestParam(defaultValue = "100") Integer limit) {
        return success(ticketAiKnowledgeService.syncClosedTickets(lastTicketId, limit));
    }

    @Operation(summary = "检索工单相似知识")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @PostMapping("/ticket/similar")
    public AjaxResult searchSimilar(@RequestParam Long ticketId) {
        return success(ticketAiKnowledgeService.searchSimilarKnowledge(ticketId));
    }

    @Operation(summary = "生成工单处理建议与回复草稿")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @PostMapping("/ticket/assist")
    public AjaxResult assist(@RequestParam Long ticketId,
                             @RequestParam(defaultValue = "5") Integer topK) {
        return success(ticketAiKnowledgeService.assist(ticketId, topK));
    }

    @Operation(summary = "生成工单 AI 分诊建议")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @PostMapping("/ticket/triage")
    public AjaxResult triage(@RequestParam Long ticketId) {
        return success(ticketAiTriageService.triage(ticketId));
    }

    @Operation(summary = "采纳 AI 分诊建议")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PostMapping("/triage/{suggestionId}/apply")
    public AjaxResult applyTriage(@PathVariable Long suggestionId,
                                  @RequestBody TicketAiTriageDecisionDTO dto) {
        ticketAiTriageService.apply(suggestionId, dto);
        return success();
    }

    @Operation(summary = "拒绝 AI 分诊建议")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PostMapping("/triage/{suggestionId}/reject")
    public AjaxResult rejectTriage(@PathVariable Long suggestionId) {
        ticketAiTriageService.reject(suggestionId);
        return success();
    }

    @Operation(summary = "提交 AI 反馈")
    @Log(title = "AI 反馈", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:feedback:add')")
    @PostMapping("/feedback")
    public AjaxResult createFeedback(@Valid @RequestBody TicketAiFeedbackDTO dto) {
        return success(ticketAiOperationsService.createFeedback(dto, getUserId()));
    }

    @Operation(summary = "查询工单 AI 反馈")
    @PreAuthorize("@ss.hasPermi('ticket:ai:feedback:list')")
    @GetMapping("/feedback/ticket/{ticketId}")
    public AjaxResult listFeedbackByTicket(@PathVariable Long ticketId) {
        return success(ticketAiOperationsService.listFeedbackByTicket(ticketId));
    }

    @Operation(summary = "AI 反馈统计")
    @PreAuthorize("@ss.hasPermi('ticket:ai:feedback:statistics')")
    @GetMapping("/feedback/statistics")
    public AjaxResult feedbackStatistics() {
        return success(ticketAiOperationsService.feedbackStatistics());
    }

    @Operation(summary = "AI 固定评测集")
    @PreAuthorize("@ss.hasPermi('ticket:ai:evaluation:list')")
    @GetMapping("/evaluation/cases")
    public AjaxResult evaluationCases() {
        return success(ticketAiOperationsService.evaluationCases());
    }

    @Operation(summary = "运行 AI 评测")
    @Log(title = "AI 评测", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('ticket:ai:evaluation:run')")
    @PostMapping("/evaluation/run")
    public AjaxResult runEvaluation() {
        return success(ticketAiOperationsService.runEvaluation());
    }

    @Operation(summary = "AI 评测结果")
    @PreAuthorize("@ss.hasPermi('ticket:ai:evaluation:list')")
    @GetMapping("/evaluation/results")
    public AjaxResult evaluationResults() {
        return success(ticketAiOperationsService.evaluationResults());
    }

    @Operation(summary = "AI 运营指标摘要")
    @PreAuthorize("@ss.hasPermi('ticket:ai:metrics:summary')")
    @GetMapping("/metrics/summary")
    public AjaxResult metricsSummary() {
        return success(ticketAiOperationsService.metricsSummary());
    }
}
