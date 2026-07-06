package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 分诊建议。
 */
public class TicketAiTriageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long suggestedCategoryId;
    private String suggestedPriority;
    private Long suggestedAssigneeId;
    private Double confidence;
    private String reasonSummary;
    private List<TicketAiSourceVO> sources;
    private Boolean degraded;
    private String reason;

    public Long getSuggestedCategoryId() { return suggestedCategoryId; }
    public void setSuggestedCategoryId(Long suggestedCategoryId) { this.suggestedCategoryId = suggestedCategoryId; }
    public String getSuggestedPriority() { return suggestedPriority; }
    public void setSuggestedPriority(String suggestedPriority) { this.suggestedPriority = suggestedPriority; }
    public Long getSuggestedAssigneeId() { return suggestedAssigneeId; }
    public void setSuggestedAssigneeId(Long suggestedAssigneeId) { this.suggestedAssigneeId = suggestedAssigneeId; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getReasonSummary() { return reasonSummary; }
    public void setReasonSummary(String reasonSummary) { this.reasonSummary = reasonSummary; }
    public List<TicketAiSourceVO> getSources() { return sources; }
    public void setSources(List<TicketAiSourceVO> sources) { this.sources = sources; }
    public Boolean getDegraded() { return degraded; }
    public void setDegraded(Boolean degraded) { this.degraded = degraded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
