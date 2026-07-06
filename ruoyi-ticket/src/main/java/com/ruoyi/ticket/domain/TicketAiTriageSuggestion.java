package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI 分诊建议记录。
 */
public class TicketAiTriageSuggestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long suggestionId;
    private Long ticketId;
    private Date ticketUpdatedAt;
    private Long suggestedCategoryId;
    private String suggestedPriority;
    private Long suggestedAssigneeId;
    private BigDecimal confidence;
    private String reasonSummary;
    private String sourceRefs;
    private String suggestionStatus;
    private Long finalCategoryId;
    private String finalPriority;
    private Long finalAssigneeId;
    private Long operatedBy;
    private Date operatedAt;
    private Date createTime;
    private Date updateTime;

    public Long getSuggestionId() { return suggestionId; }
    public void setSuggestionId(Long suggestionId) { this.suggestionId = suggestionId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Date getTicketUpdatedAt() { return ticketUpdatedAt; }
    public void setTicketUpdatedAt(Date ticketUpdatedAt) { this.ticketUpdatedAt = ticketUpdatedAt; }
    public Long getSuggestedCategoryId() { return suggestedCategoryId; }
    public void setSuggestedCategoryId(Long suggestedCategoryId) { this.suggestedCategoryId = suggestedCategoryId; }
    public String getSuggestedPriority() { return suggestedPriority; }
    public void setSuggestedPriority(String suggestedPriority) { this.suggestedPriority = suggestedPriority; }
    public Long getSuggestedAssigneeId() { return suggestedAssigneeId; }
    public void setSuggestedAssigneeId(Long suggestedAssigneeId) { this.suggestedAssigneeId = suggestedAssigneeId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getReasonSummary() { return reasonSummary; }
    public void setReasonSummary(String reasonSummary) { this.reasonSummary = reasonSummary; }
    public String getSourceRefs() { return sourceRefs; }
    public void setSourceRefs(String sourceRefs) { this.sourceRefs = sourceRefs; }
    public String getSuggestionStatus() { return suggestionStatus; }
    public void setSuggestionStatus(String suggestionStatus) { this.suggestionStatus = suggestionStatus; }
    public Long getFinalCategoryId() { return finalCategoryId; }
    public void setFinalCategoryId(Long finalCategoryId) { this.finalCategoryId = finalCategoryId; }
    public String getFinalPriority() { return finalPriority; }
    public void setFinalPriority(String finalPriority) { this.finalPriority = finalPriority; }
    public Long getFinalAssigneeId() { return finalAssigneeId; }
    public void setFinalAssigneeId(Long finalAssigneeId) { this.finalAssigneeId = finalAssigneeId; }
    public Long getOperatedBy() { return operatedBy; }
    public void setOperatedBy(Long operatedBy) { this.operatedBy = operatedBy; }
    public Date getOperatedAt() { return operatedAt; }
    public void setOperatedAt(Date operatedAt) { this.operatedAt = operatedAt; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
