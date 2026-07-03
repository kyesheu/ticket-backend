package com.ruoyi.ticket.domain;

import java.io.Serial;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 工单流程Instance实体
 *
 * @author ticket
 */
public class TicketWorkflowInstance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例 ID */
    private Long instanceId;

    /** 工单 ID */
    private Long ticketId;

    /** 流程定义 ID */
    private Long definitionId;

    /** 流程实例状态 */
    private String workflowStatus;

    /** 当前节点标识 */
    private String currentNodeKey;

    /** 启动时间 */
    private Date startedAt;

    /** 结束时间 */
    private Date endedAt;

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getCurrentNodeKey() {
        return currentNodeKey;
    }

    public void setCurrentNodeKey(String currentNodeKey) {
        this.currentNodeKey = currentNodeKey;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }
}
