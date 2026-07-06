package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiKnowledgeService;
import com.ruoyi.ticket.service.ITicketAiTriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
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

    public TicketAiController(ITicketAiDocumentService ticketAiDocumentService,
                              ITicketAiKnowledgeService ticketAiKnowledgeService,
                              ITicketAiTriageService ticketAiTriageService) {
        this.ticketAiDocumentService = ticketAiDocumentService;
        this.ticketAiKnowledgeService = ticketAiKnowledgeService;
        this.ticketAiTriageService = ticketAiTriageService;
    }

    @Operation(summary = "导入知识文档")
    @Log(title = "AI 知识文档", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:import')")
    @PostMapping("/document/import")
    public AjaxResult importDocument(@RequestParam String sourceId, @RequestParam("file") MultipartFile file) {
        return success(ticketAiDocumentService.importDocument(sourceId, file));
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
    public AjaxResult applyTriage(@org.springframework.web.bind.annotation.PathVariable Long suggestionId,
                                  @RequestBody TicketAiTriageDecisionDTO dto) {
        ticketAiTriageService.apply(suggestionId, dto);
        return success();
    }

    @Operation(summary = "拒绝 AI 分诊建议")
    @PreAuthorize("@ss.hasPermi('ticket:ticket:assign')")
    @PostMapping("/triage/{suggestionId}/reject")
    public AjaxResult rejectTriage(@org.springframework.web.bind.annotation.PathVariable Long suggestionId) {
        ticketAiTriageService.reject(suggestionId);
        return success();
    }
}
