package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单流程节点请求参数
 *
 * @author ticket
 */
public class TicketWorkflowNodeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "节点标识不能为空")
    private String nodeKey;

    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    private String assigneeType;
    private Long assigneeValue;
    private Integer sortOrder;

    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getAssigneeType() { return assigneeType; }
    public void setAssigneeType(String assigneeType) { this.assigneeType = assigneeType; }
    public Long getAssigneeValue() { return assigneeValue; }
    public void setAssigneeValue(Long assigneeValue) { this.assigneeValue = assigneeValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
