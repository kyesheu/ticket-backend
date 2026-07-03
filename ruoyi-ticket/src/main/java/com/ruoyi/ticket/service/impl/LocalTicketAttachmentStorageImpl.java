package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.ticket.service.ITicketAttachmentStorage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于 RuoYi 本地目录的工单附件存储实现。
 */
@Service
public class LocalTicketAttachmentStorageImpl implements ITicketAttachmentStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTicketAttachmentStorageImpl.class);

    @Override
    public String store(MultipartFile file) throws IOException {
        return FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
    }

    @Override
    public Resource load(String storagePath) {
        Path filePath = resolveStoragePath(storagePath);
        if (!Files.isRegularFile(filePath)) {
            throw new ServiceException("附件文件不存在");
        }
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException exception) {
            throw new ServiceException("附件文件路径无效");
        }
    }

    @Override
    public boolean delete(String storagePath) {
        try {
            Files.deleteIfExists(resolveStoragePath(storagePath));
            return true;
        } catch (IOException exception) {
            LOGGER.warn("清理工单附件文件失败，storagePath={}", storagePath, exception);
            return false;
        }
    }

    private Path resolveStoragePath(String storagePath) {
        String resourcePrefix = Constants.RESOURCE_PREFIX + "/upload/";
        if (StringUtils.isBlank(storagePath) || !storagePath.startsWith(resourcePrefix)) {
            throw new ServiceException("附件文件路径无效");
        }
        String relativePath = storagePath.substring(resourcePrefix.length());
        Path uploadRoot = Path.of(RuoYiConfig.getUploadPath()).toAbsolutePath().normalize();
        Path filePath = uploadRoot.resolve(relativePath).normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new ServiceException("附件文件路径无效");
        }
        return filePath;
    }
}
