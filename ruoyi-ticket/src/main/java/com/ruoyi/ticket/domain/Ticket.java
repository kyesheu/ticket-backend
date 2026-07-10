package com.ruoyi.ticket.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 工单主表实体
 *
 * @author ticket
 */
public class Ticket extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long ticketId;

    /** 工单编号 */
    private String ticketNo;

    /** 工单标题 */
    private String title;

    /** 工单内容 */
    private String content;

    /** 分类ID */
    private Long categoryId;

    /** 优先级：LOW/MEDIUM/HIGH/URGENT */
    private String priority;

    /** 状态：NEW/PROCESSING/WAIT_CONFIRM/CLOSED/CANCELLED */
    private String status;

    /** 创建人ID */
    private Long creatorId;

    /** 指派人ID */
    private Long assigneeId;

    /** 创建人部门ID */
    private Long deptId;

    /** 首次进入处理中的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processedAt;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedAt;

    /** 首次响应截止时间快照 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date responseDueAt;

    /** 解决截止时间快照 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resolveDueAt;

    /** 响应是否超时：0 未超时，1 已超时 */
    private String responseOverdue;

    /** 解决是否超时：0 未超时，1 已超时 */
    private String resolveOverdue;

    /** 来源类型：MANUAL/AI_ESCALATION */
    private String sourceType;

    /** 来源 AI 问答会话 ID */
    private Long aiSessionId;

    /** AI 问答摘要 */
    private String aiSummary;

    /** 分派方式：MANUAL/AI_AUTO */
    private String dispatchMode;

    /** 分派原因 */
    private String dispatchReason;

    /** 删除标志：0存在 2删除 */
    private String delFlag;

    // ---- 以下为 LEFT JOIN 查询时填充的展示字段 ----

    /** 创建人昵称 */
    private String creatorName;

    /** 指派人昵称 */
    private String assigneeName;

    /** 部门名称 */
    private String deptName;

    /** 分类名称 */
    private String categoryName;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Date getResponseDueAt() {
        return responseDueAt;
    }

    public void setResponseDueAt(Date responseDueAt) {
        this.responseDueAt = responseDueAt;
    }

    public Date getResolveDueAt() {
        return resolveDueAt;
    }

    public void setResolveDueAt(Date resolveDueAt) {
        this.resolveDueAt = resolveDueAt;
    }

    public String getResponseOverdue() {
        return responseOverdue;
    }

    public void setResponseOverdue(String responseOverdue) {
        this.responseOverdue = responseOverdue;
    }

    public String getResolveOverdue() {
        return resolveOverdue;
    }

    public void setResolveOverdue(String resolveOverdue) {
        this.resolveOverdue = resolveOverdue;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getAiSessionId() {
        return aiSessionId;
    }

    public void setAiSessionId(Long aiSessionId) {
        this.aiSessionId = aiSessionId;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getDispatchMode() {
        return dispatchMode;
    }

    public void setDispatchMode(String dispatchMode) {
        this.dispatchMode = dispatchMode;
    }

    public String getDispatchReason() {
        return dispatchReason;
    }

    public void setDispatchReason(String dispatchReason) {
        this.dispatchReason = dispatchReason;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
