package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * AI 结果引用来源。
 */
public class TicketAiSourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sourceType;
    private String sourceId;
    private String title;
    private String snippet;
    private Double score;
    private Map<String, Object> metadata;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
