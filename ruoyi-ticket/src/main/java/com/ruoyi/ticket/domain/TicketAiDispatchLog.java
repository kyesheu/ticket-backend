package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.io.Serial;
import java.math.BigDecimal;

/**
 * AI 自动分派审计日志。
 */
public class TicketAiDispatchLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long ticketId;
    private Long sessionId;
    private Long suggestedCategoryId;
    private String suggestedPriority;
    private Long suggestedAssigneeId;
    private BigDecimal confidence;
    private String decision;
    private String reason;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSuggestedCategoryId() {
        return suggestedCategoryId;
    }

    public void setSuggestedCategoryId(Long suggestedCategoryId) {
        this.suggestedCategoryId = suggestedCategoryId;
    }

    public String getSuggestedPriority() {
        return suggestedPriority;
    }

    public void setSuggestedPriority(String suggestedPriority) {
        this.suggestedPriority = suggestedPriority;
    }

    public Long getSuggestedAssigneeId() {
        return suggestedAssigneeId;
    }

    public void setSuggestedAssigneeId(Long suggestedAssigneeId) {
        this.suggestedAssigneeId = suggestedAssigneeId;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
