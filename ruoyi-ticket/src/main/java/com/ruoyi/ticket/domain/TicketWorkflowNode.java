package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单流程Node实体
 *
 * @author ticket
 */
public class TicketWorkflowNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 ID */
    private Long nodeId;

    /** 流程定义 ID */
    private Long definitionId;

    /** 节点标识 */
    private String nodeKey;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型 */
    private String nodeType;

    /** 处理人类型 */
    private String assigneeType;

    /** 处理人配置值 */
    private Long assigneeValue;

    /** 排序 */
    private Integer sortOrder;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
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

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public void setAssigneeType(String assigneeType) {
        this.assigneeType = assigneeType;
    }

    public Long getAssigneeValue() {
        return assigneeValue;
    }

    public void setAssigneeValue(Long assigneeValue) {
        this.assigneeValue = assigneeValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
