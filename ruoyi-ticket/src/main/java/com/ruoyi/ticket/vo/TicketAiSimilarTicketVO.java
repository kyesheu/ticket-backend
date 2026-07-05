package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 相似历史工单。
 */
public class TicketAiSimilarTicketVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private String title;
    private String category;
    private String solution;
    private Double score;
    private Long sourceGeneration;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Long getSourceGeneration() { return sourceGeneration; }
    public void setSourceGeneration(Long sourceGeneration) { this.sourceGeneration = sourceGeneration; }
}
