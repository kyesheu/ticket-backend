package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.config.TicketAiProperties;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.service.ITicketAiDocumentService;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文档导入 Service 实现。
 */
public class TicketAiDocumentServiceImpl implements ITicketAiDocumentService {

    private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "txt", "text/plain",
            "md", "text/markdown",
            "pdf", "application/pdf");

    private final ITicketAiService ticketAiService;
    private final TicketAiProperties properties;

    public TicketAiDocumentServiceImpl(ITicketAiService ticketAiService, TicketAiProperties properties) {
        this.ticketAiService = ticketAiService;
        this.properties = properties;
    }

    @Override
    public TicketAiAcceptedVO importDocument(String sourceId, MultipartFile file) {
        if (!StringUtils.hasText(sourceId) || !SOURCE_ID_PATTERN.matcher(sourceId).matches()) {
            throw new ServiceException("知识文档来源标识格式错误");
        }
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
            dto.setFileName(fileName);
            dto.setContentType(expectedContentType);
            dto.setContentBase64(Base64.getEncoder().encodeToString(file.getBytes()));
            return ticketAiService.importDocument(dto);
        } catch (IOException exception) {
            throw new ServiceException("读取知识文档失败");
        }
    }
}
