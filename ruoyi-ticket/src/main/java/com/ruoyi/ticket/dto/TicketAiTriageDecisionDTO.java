package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 分诊建议确认请求。
 */
public class TicketAiTriageDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String priority;
    private Long assigneeId;
    private String comment;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
