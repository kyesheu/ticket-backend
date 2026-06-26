package com.ruoyi.ticket.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketOperationLog;

/**
 * 工单详情响应体
 *
 * @author ticket
 */
public class TicketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private String ticketNo;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private String priority;
    private String status;
    private Long creatorId;
    private String creatorName;
    private Long assigneeId;
    private String assigneeName;
    private Long deptId;
    private String deptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    private String remark;

    /** 评论列表，按时间倒序 */
    private List<TicketComment> comments;

    /** 操作日志，按时间倒序 */
    private List<TicketOperationLog> logs;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Date getProcessedAt() { return processedAt; }
    public void setProcessedAt(Date processedAt) { this.processedAt = processedAt; }

    public Date getClosedAt() { return closedAt; }
    public void setClosedAt(Date closedAt) { this.closedAt = closedAt; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<TicketComment> getComments() { return comments; }
    public void setComments(List<TicketComment> comments) { this.comments = comments; }

    public List<TicketOperationLog> getLogs() { return logs; }
    public void setLogs(List<TicketOperationLog> logs) { this.logs = logs; }
}
