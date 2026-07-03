package com.ruoyi.ticket.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 工单附件物理存储接口。
 */
public interface ITicketAttachmentStorage {
    String store(MultipartFile file) throws IOException;

    Resource load(String storagePath);

    boolean delete(String storagePath);
}
