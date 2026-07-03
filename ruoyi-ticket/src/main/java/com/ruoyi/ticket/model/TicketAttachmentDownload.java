package com.ruoyi.ticket.model;

import org.springframework.core.io.Resource;

/**
 * 工单附件下载内容。
 */
public class TicketAttachmentDownload {
    private final String originalName;
    private final String contentType;
    private final Resource resource;

    public TicketAttachmentDownload(String originalName, String contentType, Resource resource) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.resource = resource;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public Resource getResource() {
        return resource;
    }
}
