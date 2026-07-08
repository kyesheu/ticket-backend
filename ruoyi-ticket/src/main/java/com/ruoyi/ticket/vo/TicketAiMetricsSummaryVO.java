package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 运营指标摘要。
 */
public class TicketAiMetricsSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private TicketAiFeedbackStatisticsVO feedback;
    private Long triageSuggestionCount;
    private Long triageAppliedCount;
    private Long triageRejectedCount;

    public TicketAiFeedbackStatisticsVO getFeedback() { return feedback; }
    public void setFeedback(TicketAiFeedbackStatisticsVO feedback) { this.feedback = feedback; }
    public Long getTriageSuggestionCount() { return triageSuggestionCount; }
    public void setTriageSuggestionCount(Long triageSuggestionCount) { this.triageSuggestionCount = triageSuggestionCount; }
    public Long getTriageAppliedCount() { return triageAppliedCount; }
    public void setTriageAppliedCount(Long triageAppliedCount) { this.triageAppliedCount = triageAppliedCount; }
    public Long getTriageRejectedCount() { return triageRejectedCount; }
    public void setTriageRejectedCount(Long triageRejectedCount) { this.triageRejectedCount = triageRejectedCount; }
}
