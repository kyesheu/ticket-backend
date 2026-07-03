package com.ruoyi.ticket.service;

/**
 * 工单附件物理清理 Service 接口。
 */
public interface ITicketAttachmentCleanupService {
    void cleanup(Long attachmentId, String storagePath);
}
