package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 前置问答结果。
 */
public class TicketAiQuestionAnswerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String answer;
    private String suggestion;
    private Double confidence;
    private Boolean needHuman;
    private List<TicketAiSourceVO> sources;
    private Boolean degraded;
    private String reason;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Boolean getNeedHuman() { return needHuman; }
    public void setNeedHuman(Boolean needHuman) { this.needHuman = needHuman; }
    public List<TicketAiSourceVO> getSources() { return sources; }
    public void setSources(List<TicketAiSourceVO> sources) { this.sources = sources; }
    public Boolean getDegraded() { return degraded; }
    public void setDegraded(Boolean degraded) { this.degraded = degraded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
