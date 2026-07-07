package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识文档详情。
 */
public class TicketAiDocumentDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sourceId;
    private String title;
    private String status;
    private Integer chunkCount;
    private String summary;
    private Date lastImportedAt;
    private String lastImportResult;
    private String failureReasonSummary;

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Date getLastImportedAt() { return lastImportedAt; }
    public void setLastImportedAt(Date lastImportedAt) { this.lastImportedAt = lastImportedAt; }
    public String getLastImportResult() { return lastImportResult; }
    public void setLastImportResult(String lastImportResult) { this.lastImportResult = lastImportResult; }
    public String getFailureReasonSummary() { return failureReasonSummary; }
    public void setFailureReasonSummary(String failureReasonSummary) {
        this.failureReasonSummary = failureReasonSummary;
    }
}
