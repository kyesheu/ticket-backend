package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.mapper.TicketAttachmentMapper;
import com.ruoyi.ticket.service.ITicketAttachmentCleanupService;
import com.ruoyi.ticket.service.ITicketAttachmentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单附件物理清理 Service 实现。
 */
@Service
public class TicketAttachmentCleanupServiceImpl implements ITicketAttachmentCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketAttachmentCleanupServiceImpl.class);

    @Autowired
    private TicketAttachmentMapper ticketAttachmentMapper;

    @Autowired
    private ITicketAttachmentStorage ticketAttachmentStorage;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void cleanup(Long attachmentId, String storagePath) {
        if (!ticketAttachmentStorage.delete(storagePath)) {
            LOGGER.error("工单附件物理文件清理失败，attachmentId={}，storagePath={}", attachmentId, storagePath);
            return;
        }
        int rows = ticketAttachmentMapper.markStorageDeleted(attachmentId);
        if (rows != 1) {
            LOGGER.error("工单附件物理清理状态更新失败，attachmentId={}", attachmentId);
        }
    }
}
