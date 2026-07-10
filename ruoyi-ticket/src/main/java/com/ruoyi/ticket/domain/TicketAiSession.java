package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI 前置问答会话。
 */
public class TicketAiSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private Long userId;
    private String question;
    private String answer;
    private String suggestion;
    private BigDecimal confidence;
    private String needHuman;
    private String degraded;
    private String reason;
    private String status;
    private Long ticketId;
    private Date createTime;
    private Date updateTime;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getNeedHuman() { return needHuman; }
    public void setNeedHuman(String needHuman) { this.needHuman = needHuman; }
    public String getDegraded() { return degraded; }
    public void setDegraded(String degraded) { this.degraded = degraded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
