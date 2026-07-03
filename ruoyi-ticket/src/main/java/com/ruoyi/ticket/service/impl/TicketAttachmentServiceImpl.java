package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.file.FileSizeLimitExceededException;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketAttachment;
import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketAttachmentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAttachmentDownload;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAttachmentService;
import com.ruoyi.ticket.service.ITicketAttachmentStorage;
import com.ruoyi.ticket.service.ITicketAttachmentCleanupService;
import com.ruoyi.ticket.vo.TicketAttachmentVO;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Objects;

/**
 * 工单附件 Service 实现。
 */
@Service
public class TicketAttachmentServiceImpl implements ITicketAttachmentService {
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_ATTACHMENTS_PER_TICKET = 20;
    private static final String NORMAL_FLAG = "0";
    private static final String LIST_PERMISSION = "ticket:attachment:list";
    private static final String DOWNLOAD_PERMISSION = "ticket:attachment:download";
    private static final String REMOVE_PERMISSION = "ticket:attachment:remove";

    @Autowired
    private TicketAttachmentMapper ticketAttachmentMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private ITicketAttachmentStorage ticketAttachmentStorage;

    @Autowired
    private ITicketAttachmentCleanupService ticketAttachmentCleanupService;

    @Autowired
    private ITicketAccessPolicy ticketAccessPolicy;

    @Override
    public TicketAttachmentVO uploadTemporary(MultipartFile file) throws IOException {
        validateFile(file);
        String originalName = file.getOriginalFilename();
        String storagePath = ticketAttachmentStorage.store(file);
        try {
            TicketAttachment attachment = buildTemporaryAttachment(file, originalName, storagePath);
            int rows = ticketAttachmentMapper.insertAttachment(attachment);
            if (rows != 1) {
                throw new ServiceException("附件元数据保存失败");
            }
            return toVO(attachment);
        } catch (RuntimeException exception) {
            ticketAttachmentStorage.delete(storagePath);
            throw exception;
        }
    }

    @Override
    public List<TicketAttachmentVO> selectByTicketId(Long ticketId) {
        ticketAccessPolicy.assertCanAccess(ticketId, LIST_PERMISSION);
        return ticketAttachmentMapper.selectByTicketId(ticketId).stream().map(this::toVO).toList();
    }

    @Override
    public TicketAttachmentDownload loadForDownload(Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentMapper.selectById(attachmentId);
        if (attachment == null || attachment.getTicketId() == null) {
            throw new ServiceException("附件不存在");
        }
        ticketAccessPolicy.assertCanAccess(attachment.getTicketId(), DOWNLOAD_PERMISSION);
        return new TicketAttachmentDownload(attachment.getOriginalName(), attachment.getContentType(),
                ticketAttachmentStorage.load(attachment.getStoragePath()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindAttachments(Long ticketId, TicketAttachmentBusinessType businessType,
                                Long businessId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        validateBindingArguments(ticketId, businessType, businessId, attachmentIds);
        Long currentUserId = SecurityUtils.getUserId();
        List<TicketAttachment> attachments = ticketAttachmentMapper.selectByIds(attachmentIds);
        if (attachments.size() != attachmentIds.size()
                || attachments.stream().anyMatch(attachment -> !isBindableBy(attachment, currentUserId))) {
            throw new ServiceException("附件不存在或不可绑定");
        }
        int existingCount = ticketAttachmentMapper.countByTicketId(ticketId);
        if (existingCount + attachmentIds.size() > MAX_ATTACHMENTS_PER_TICKET) {
            throw new ServiceException("每个工单最多绑定20个附件");
        }
        int rows = ticketAttachmentMapper.bindAttachments(ticketId, businessType.name(), businessId,
                currentUserId, attachmentIds);
        if (rows != attachmentIds.size()) {
            throw new ServiceException("附件绑定失败，请勿重复提交");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new ServiceException("附件不存在");
        }
        boolean administrator = SecurityUtils.isAdmin();
        assertDeletePermission(attachment, administrator);
        int rows = ticketAttachmentMapper.logicallyDelete(attachmentId, SecurityUtils.getUsername());
        if (rows != 1) {
            throw new ServiceException("附件不存在");
        }
        runAfterCommit(() -> ticketAttachmentCleanupService.cleanup(
                attachment.getAttachmentId(), attachment.getStoragePath()));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("附件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName)
                || originalName.length() > MAX_FILE_NAME_LENGTH
                || containsControlCharacter(originalName)
                || !originalName.equals(FilenameUtils.getName(originalName))) {
            throw new ServiceException("附件文件名无效");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("附件大小不能超过10MB");
        }
        try {
            FileUploadUtils.assertAllowed(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        } catch (FileSizeLimitExceededException exception) {
            throw new ServiceException("附件大小超过系统限制");
        } catch (InvalidExtensionException exception) {
            throw new ServiceException("不支持该附件类型");
        }
    }

    private boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    private void validateBindingArguments(Long ticketId, TicketAttachmentBusinessType businessType,
                                          Long businessId, List<Long> attachmentIds) {
        if (ticketId == null || businessType == null || businessId == null) {
            throw new ServiceException("附件绑定参数不能为空");
        }
        if (attachmentIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(attachmentIds).size() != attachmentIds.size()) {
            throw new ServiceException("附件ID不能为空或重复");
        }
    }

    private boolean isBindableBy(TicketAttachment attachment, Long currentUserId) {
        return Objects.equals(attachment.getUploaderId(), currentUserId)
                && attachment.getTicketId() == null
                && attachment.getBusinessType() == null
                && attachment.getBusinessId() == null;
    }

    private void assertDeletePermission(TicketAttachment attachment, boolean administrator) {
        if (attachment.getTicketId() != null) {
            ticketAccessPolicy.assertCanAccess(attachment.getTicketId(), REMOVE_PERMISSION);
            Ticket ticket = ticketMapper.selectTicketEntityById(attachment.getTicketId());
            if (ticket == null) {
                throw new ServiceException("工单不存在");
            }
            if (!administrator && isTerminal(ticket.getStatus())) {
                throw new ServiceException("终态工单附件不可删除");
            }
        }
        if (!administrator && !Objects.equals(attachment.getUploaderId(), SecurityUtils.getUserId())) {
            throw new ServiceException("只能删除本人上传的附件");
        }
    }

    private boolean isTerminal(String status) {
        return TicketStatus.CLOSED.name().equals(status) || TicketStatus.CANCELLED.name().equals(status);
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }


    private TicketAttachment buildTemporaryAttachment(MultipartFile file, String originalName, String storagePath) {
        TicketAttachment attachment = new TicketAttachment();
        attachment.setOriginalName(originalName);
        attachment.setStoragePath(storagePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileExtension(FileUploadUtils.getExtension(file).toLowerCase(Locale.ROOT));
        attachment.setFileSize(file.getSize());
        attachment.setUploaderId(SecurityUtils.getUserId());
        attachment.setCreateBy(SecurityUtils.getUsername());
        attachment.setCreateTime(new Date());
        attachment.setDeletedFlag(NORMAL_FLAG);
        return attachment;
    }

    private TicketAttachmentVO toVO(TicketAttachment attachment) {
        TicketAttachmentVO vo = new TicketAttachmentVO();
        vo.setAttachmentId(attachment.getAttachmentId());
        vo.setBusinessType(attachment.getBusinessType());
        vo.setBusinessId(attachment.getBusinessId());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setContentType(attachment.getContentType());
        vo.setFileExtension(attachment.getFileExtension());
        vo.setFileSize(attachment.getFileSize());
        vo.setUploaderId(attachment.getUploaderId());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }
}
