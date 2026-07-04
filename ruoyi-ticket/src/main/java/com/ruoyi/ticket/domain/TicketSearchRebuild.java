package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 工单检索全量重建状态。 */
public class TicketSearchRebuild implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long rebuildId;
    private String rebuildStatus;
    private String indexName;
    private Long totalCount;
    private Long processedCount;
    private Long lastTicketId;
    private Long startEventId;
    private Long maxTicketId;
    private String errorMessage;
    private Date startedAt;
    private Date endedAt;
    private Date updateTime;
    public Long getRebuildId() { return rebuildId; }
    public void setRebuildId(Long rebuildId) { this.rebuildId = rebuildId; }
    public String getRebuildStatus() { return rebuildStatus; }
    public void setRebuildStatus(String rebuildStatus) { this.rebuildStatus = rebuildStatus; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public Long getProcessedCount() { return processedCount; }
    public void setProcessedCount(Long processedCount) { this.processedCount = processedCount; }
    public Long getLastTicketId() { return lastTicketId; }
    public void setLastTicketId(Long lastTicketId) { this.lastTicketId = lastTicketId; }
    public Long getStartEventId() { return startEventId; }
    public void setStartEventId(Long startEventId) { this.startEventId = startEventId; }
    public Long getMaxTicketId() { return maxTicketId; }
    public void setMaxTicketId(Long maxTicketId) { this.maxTicketId = maxTicketId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getEndedAt() { return endedAt; }
    public void setEndedAt(Date endedAt) { this.endedAt = endedAt; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
