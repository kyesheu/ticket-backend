package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI 问答引用来源。
 */
public class TicketAiSessionSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sourceRefId;
    private Long sessionId;
    private String sourceType;
    private String sourceId;
    private String title;
    private String snippet;
    private BigDecimal score;
    private String metadataJson;
    private Date createTime;

    public Long getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(Long sourceRefId) { this.sourceRefId = sourceRefId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
