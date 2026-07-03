package com.ruoyi.ticket.domain;

import java.io.Serial;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单流程Definition实体
 *
 * @author ticket
 */
public class TicketWorkflowDefinition extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 流程定义 ID */
    private Long definitionId;

    /** 流程稳定标识 */
    private String workflowKey;

    /** 流程名称 */
    private String workflowName;

    /** 版本号 */
    private Integer version;

    /** 定义状态 */
    private String definitionStatus;

    /** 是否为当前发布版本 */
    private String current;

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(String workflowKey) {
        this.workflowKey = workflowKey;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDefinitionStatus() {
        return definitionStatus;
    }

    public void setDefinitionStatus(String definitionStatus) {
        this.definitionStatus = definitionStatus;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }
}
