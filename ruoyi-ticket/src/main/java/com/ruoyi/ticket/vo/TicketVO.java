package com.ruoyi.ticket.vo;
import java.io.Serial;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单详情响应体
 *
 * @author ticket
 */
@Schema(description = "工单详情")
public class TicketVO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long ticketId;

    @Schema(description = "工单编号")
    private String ticketNo;

    @Schema(description = "工单标题")
    private String title;

    @Schema(description = "工单内容")
    private String content;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "优先级：LOW/MEDIUM/HIGH/URGENT")
    private String priority;

    @Schema(description = "状态：NEW/PROCESSING/WAIT_CONFIRM/CLOSED/CANCELLED")
    private String status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorName;

    @Schema(description = "指派人ID")
    private Long assigneeId;

    @Schema(description = "指派人昵称")
    private String assigneeName;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedAt;

    @Schema(description = "关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedAt;

    @Schema(description = "首次响应截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date responseDueAt;

    @Schema(description = "解决截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resolveDueAt;

    @Schema(description = "响应是否超时：0否 1是")
    private String responseOverdue;

    @Schema(description = "解决是否超时：0否 1是")
    private String resolveOverdue;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "评论列表")
    private List<TicketComment> comments;

    @Schema(description = "操作日志列表")
    private List<TicketOperationLog> logs;

    @Schema(description = "自定义字段值快照")
    private List<TicketCustomFieldValue> customFields;

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
    public Date getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(Date responseDueAt) { this.responseDueAt = responseDueAt; }
    public Date getResolveDueAt() { return resolveDueAt; }
    public void setResolveDueAt(Date resolveDueAt) { this.resolveDueAt = resolveDueAt; }
    public String getResponseOverdue() { return responseOverdue; }
    public void setResponseOverdue(String responseOverdue) { this.responseOverdue = responseOverdue; }
    public String getResolveOverdue() { return resolveOverdue; }
    public void setResolveOverdue(String resolveOverdue) { this.resolveOverdue = resolveOverdue; }
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
    public List<TicketCustomFieldValue> getCustomFields() { return customFields; }
    public void setCustomFields(List<TicketCustomFieldValue> customFields) { this.customFields = customFields; }
}
