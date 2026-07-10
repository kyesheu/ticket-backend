package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.config.TicketAiProperties;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import com.ruoyi.ticket.vo.TicketAiDocumentDetailVO;
import com.ruoyi.ticket.vo.TicketAiDocumentListVO;
import com.ruoyi.ticket.vo.TicketVO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文档导入 Service 实现。
 */
public class TicketAiDocumentServiceImpl implements ITicketAiDocumentService {

    private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final int MAX_PAGE_SIZE = 100;
    private static final String TICKET_QUERY_PERMISSION = "ticket:ticket:query";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "txt", "text/plain",
            "md", "text/markdown",
            "pdf", "application/pdf");

    private final ITicketAiService ticketAiService;
    private final TicketAiProperties properties;
    private final TicketMapper ticketMapper;
    private final TicketOperationLogMapper operationLogMapper;
    private final ITicketAccessPolicy accessPolicy;

    public TicketAiDocumentServiceImpl(ITicketAiService ticketAiService,
                                       TicketAiProperties properties,
                                       TicketMapper ticketMapper,
                                       TicketOperationLogMapper operationLogMapper,
                                       ITicketAccessPolicy accessPolicy) {
        this.ticketAiService = ticketAiService;
        this.properties = properties;
        this.ticketMapper = ticketMapper;
        this.operationLogMapper = operationLogMapper;
        this.accessPolicy = accessPolicy;
    }

    public TicketAiAcceptedVO importDocument(String sourceId, MultipartFile file) {
        return importDocument(sourceId, null, file);
    }

    @Override
    public TicketAiAcceptedVO importDocument(String sourceId, String categoryName, MultipartFile file) {
        validateSourceId(sourceId);
        if (file == null || file.isEmpty()) {
            throw new ServiceException("知识文档不能为空");
        }
        if (file.getSize() > properties.getMaxDocumentBytes()) {
            throw new ServiceException("知识文档大小超过限制");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(fileName) || fileName.contains("..")) {
            throw new ServiceException("知识文档文件名非法");
        }
        String extension = StringUtils.getFilenameExtension(fileName);
        String expectedContentType = extension == null ? null : CONTENT_TYPES.get(extension.toLowerCase());
        if (expectedContentType == null || !expectedContentType.equals(file.getContentType())) {
            throw new ServiceException("知识文档类型不支持或 MIME 不匹配");
        }
        try {
            TicketAiDocumentImportDTO dto = new TicketAiDocumentImportDTO();
            dto.setSourceId(sourceId);
            dto.setCategoryName(StringUtils.hasText(categoryName) ? categoryName.trim() : "未分类");
            dto.setFileName(fileName);
            dto.setContentType(expectedContentType);
            dto.setContentBase64(Base64.getEncoder().encodeToString(file.getBytes()));
            return ticketAiService.importDocument(dto);
        } catch (IOException exception) {
            throw new ServiceException("读取知识文档失败");
        }
    }

    @Override
    public TicketAiDocumentListVO listDocuments(TicketAiDocumentQueryDTO query) {
        TicketAiDocumentQueryDTO safeQuery = query == null ? new TicketAiDocumentQueryDTO() : query;
        if (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1) {
            throw new ServiceException("知识文档页码必须大于 0");
        }
        if (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1
                || safeQuery.getPageSize() > MAX_PAGE_SIZE) {
            throw new ServiceException("知识文档每页条数必须在 1 到 100 之间");
        }
        return ticketAiService.listDocuments(safeQuery);
    }

    @Override
    public TicketAiDocumentDetailVO getDocument(String sourceId) {
        validateSourceId(sourceId);
        return ticketAiService.getDocument(sourceId);
    }

    @Override
    public TicketAiAcceptedVO deleteDocument(String sourceId) {
        validateSourceId(sourceId);
        return ticketAiService.deleteDocument(sourceId);
    }

    @Override
    public TicketAiAcceptedVO reimportDocument(String sourceId) {
        validateSourceId(sourceId);
        return ticketAiService.reimportDocument(sourceId);
    }

    @Override
    public TicketAiAcceptedVO importClosedTicketKnowledge(Long ticketId) {
        accessPolicy.assertCanAccess(ticketId, TICKET_QUERY_PERMISSION);
        TicketVO ticket = ticketMapper.selectTicketById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        if (!TicketStatus.CLOSED.name().equals(ticket.getStatus())) {
            throw new ServiceException("只有已关闭工单可以沉淀为知识");
        }
        TicketOperationLog processLog = operationLogMapper.selectLatestProcessLog(ticketId);
        if (processLog == null || !StringUtils.hasText(processLog.getComment())) {
            throw new ServiceException("工单缺少有效处理结果，无法沉淀为知识");
        }

        String sourceId = "ticket-knowledge-" + ticketId;
        TicketAiDocumentImportDTO dto = new TicketAiDocumentImportDTO();
        dto.setSourceId(sourceId);
        dto.setCategoryName(StringUtils.hasText(ticket.getCategoryName()) ? ticket.getCategoryName() : "历史工单");
        dto.setFileName(sourceId + ".md");
        dto.setContentType("text/markdown");
        dto.setContentBase64(Base64.getEncoder().encodeToString(buildKnowledgeMarkdown(ticket, processLog).getBytes(StandardCharsets.UTF_8)));
        TicketAiAcceptedVO result = ticketAiService.importDocument(dto);
        insertKnowledgeLog(ticketId, sourceId);
        return result;
    }

    private void validateSourceId(String sourceId) {
        if (!StringUtils.hasText(sourceId) || !SOURCE_ID_PATTERN.matcher(sourceId).matches()) {
            throw new ServiceException("知识文档来源标识格式错误");
        }
    }

    private String buildKnowledgeMarkdown(TicketVO ticket, TicketOperationLog processLog) {
        StringBuilder content = new StringBuilder();
        content.append("# ").append(clean(ticket.getTitle())).append("\n\n");
        content.append("## 来源工单\n\n");
        content.append("- 工单编号：").append(clean(ticket.getTicketNo())).append("\n");
        content.append("- 来源工单ID：").append(ticket.getTicketId()).append("\n");
        content.append("- 分类：").append(clean(ticket.getCategoryName())).append("\n");
        content.append("- 优先级：").append(clean(ticket.getPriority())).append("\n\n");
        content.append("## 问题描述\n\n").append(clean(ticket.getContent())).append("\n\n");
        content.append("## 处理方案\n\n").append(clean(processLog.getComment())).append("\n\n");
        content.append("## 适用场景\n\n");
        content.append("当用户反馈类似“").append(clean(ticket.getTitle())).append("”的问题时，可参考本处理方案。\n");
        return content.toString();
    }

    private String clean(String text) {
        return StringUtils.hasText(text) ? text.trim() : "-";
    }

    private void insertKnowledgeLog(Long ticketId, String sourceId) {
        TicketOperationLog log = new TicketOperationLog();
        log.setTicketId(ticketId);
        log.setOperationType("KNOWLEDGE");
        log.setFromStatus(TicketStatus.CLOSED.name());
        log.setToStatus(TicketStatus.CLOSED.name());
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setComment("沉淀为知识库文档：" + sourceId);
        log.setOperateTime(new Date());
        operationLogMapper.insertLog(log);
    }
}
