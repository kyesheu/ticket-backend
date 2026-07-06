package com.ruoyi.ticket.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 可同步的历史已关闭工单查询投影。
 */
public class TicketAiSyncCandidate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private String title;
    private String category;
    private String description;
    private String solution;
    private String status;
    private String priority;
    private Date createdTime;
    private Date closedTime;
    private Long sourceGeneration;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }
    public Date getClosedTime() { return closedTime; }
    public void setClosedTime(Date closedTime) { this.closedTime = closedTime; }
    public Long getSourceGeneration() { return sourceGeneration; }
    public void setSourceGeneration(Long sourceGeneration) { this.sourceGeneration = sourceGeneration; }
}
