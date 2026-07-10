package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 问答转人工记录。
 */
public class TicketAiEscalation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long escalationId;
    private Long sessionId;
    private Long ticketId;
    private Long userId;
    private String userComment;
    private String aiSummary;
    private Date createTime;

    public Long getEscalationId() { return escalationId; }
    public void setEscalationId(Long escalationId) { this.escalationId = escalationId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserComment() { return userComment; }
    public void setUserComment(String userComment) { this.userComment = userComment; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
