package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.service.ITicketAiDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
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

    public TicketAiController(ITicketAiDocumentService ticketAiDocumentService) {
        this.ticketAiDocumentService = ticketAiDocumentService;
    }

    @Operation(summary = "导入知识文档")
    @Log(title = "AI 知识文档", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ai:document:import')")
    @PostMapping("/document/import")
    public AjaxResult importDocument(@RequestParam String sourceId, @RequestParam("file") MultipartFile file) {
        return success(ticketAiDocumentService.importDocument(sourceId, file));
    }
}
