package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单附件元数据实体。
 */
public class TicketAttachment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long attachmentId;
    private Long ticketId;
    private String businessType;
    private Long businessId;
    private String originalName;
    private String storagePath;
    private String contentType;
    private String fileExtension;
    private Long fileSize;
    private Long uploaderId;
    private String createBy;
    private Date createTime;
    private String deletedFlag;
    private String deleteBy;
    private Date deleteTime;
    private String storageDeletedFlag;
    private Date storageDeleteTime;

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(String deletedFlag) { this.deletedFlag = deletedFlag; }
    public String getDeleteBy() { return deleteBy; }
    public void setDeleteBy(String deleteBy) { this.deleteBy = deleteBy; }
    public Date getDeleteTime() { return deleteTime; }
    public void setDeleteTime(Date deleteTime) { this.deleteTime = deleteTime; }
    public String getStorageDeletedFlag() { return storageDeletedFlag; }
    public void setStorageDeletedFlag(String storageDeletedFlag) { this.storageDeletedFlag = storageDeletedFlag; }
    public Date getStorageDeleteTime() { return storageDeleteTime; }
    public void setStorageDeleteTime(Date storageDeleteTime) { this.storageDeleteTime = storageDeleteTime; }
}
