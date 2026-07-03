package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工单流程Task实体
 *
 * @author ticket
 */
public class TicketWorkflowTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务 ID */
    private Long taskId;

    /** 流程实例 ID */
    private Long instanceId;

    /** 节点标识快照 */
    private String nodeKey;

    /** 节点名称快照 */
    private String nodeName;

    /** 任务状态 */
    private String taskStatus;

    /** 处理人类型 */
    private String assigneeType;

    /** 处理人配置引用 ID */
    private Long assigneeRefId;

    /** 解析后的处理用户 ID */
    private Long resolvedAssigneeId;

    /** 实际处理用户 ID */
    private Long completedBy;

    /** 处理动作 */
    private String actionType;

    /** 处理意见 */
    private String comment;

    /** 任务创建时间 */
    private Date createdAt;

    /** 任务完成时间 */
    private Date completedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public void setAssigneeType(String assigneeType) {
        this.assigneeType = assigneeType;
    }

    public Long getAssigneeRefId() {
        return assigneeRefId;
    }

    public void setAssigneeRefId(Long assigneeRefId) {
        this.assigneeRefId = assigneeRefId;
    }

    public Long getResolvedAssigneeId() {
        return resolvedAssigneeId;
    }

    public void setResolvedAssigneeId(Long resolvedAssigneeId) {
        this.resolvedAssigneeId = resolvedAssigneeId;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }
}
