package com.ruoyi.ticket.vo;
import java.io.Serial;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单列表响应体
 *
 * @author ticket
 */
@Schema(description = "工单列表项")
public class TicketListVO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long ticketId;

    @Schema(description = "工单编号")
    private String ticketNo;

    @Schema(description = "工单标题")
    private String title;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "创建人昵称")
    private String creatorName;

    @Schema(description = "指派人昵称")
    private String assigneeName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

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

    @Schema(description = "分派方式：MANUAL/AI_AUTO")
    private String dispatchMode;

    @Schema(description = "分派原因")
    private String dispatchReason;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(Date responseDueAt) { this.responseDueAt = responseDueAt; }
    public Date getResolveDueAt() { return resolveDueAt; }
    public void setResolveDueAt(Date resolveDueAt) { this.resolveDueAt = resolveDueAt; }
    public String getResponseOverdue() { return responseOverdue; }
    public void setResponseOverdue(String responseOverdue) { this.responseOverdue = responseOverdue; }
    public String getResolveOverdue() { return resolveOverdue; }
    public void setResolveOverdue(String resolveOverdue) { this.resolveOverdue = resolveOverdue; }
    public String getDispatchMode() { return dispatchMode; }
    public void setDispatchMode(String dispatchMode) { this.dispatchMode = dispatchMode; }
    public String getDispatchReason() { return dispatchReason; }
    public void setDispatchReason(String dispatchReason) { this.dispatchReason = dispatchReason; }
}
