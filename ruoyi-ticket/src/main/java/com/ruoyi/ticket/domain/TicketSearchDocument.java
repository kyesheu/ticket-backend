package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单 Elasticsearch 索引文档。
 */
public class TicketSearchDocument implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private Long ticketId;
    private Long sourceEventId;
    private String ticketNo;
    private String title;
    private String content;
    private String comments;
    private Long categoryId;
    private String priority;
    private String status;
    private Long creatorId;
    private Long assigneeId;
    private Long deptId;
    private Date createTime;
    private Date updateTime;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(Long sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
