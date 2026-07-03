package com.ruoyi.ticket.vo;

import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工单流程定义响应对象
 *
 * @author ticket
 */
public class TicketWorkflowDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long definitionId;
    private String workflowKey;
    private String workflowName;
    private Integer version;
    private String definitionStatus;
    private String current;
    private String remark;
    private Date createTime;
    private List<TicketWorkflowNode> nodes;
    private List<TicketWorkflowTransition> transitions;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getWorkflowKey() { return workflowKey; }
    public void setWorkflowKey(String workflowKey) { this.workflowKey = workflowKey; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDefinitionStatus() { return definitionStatus; }
    public void setDefinitionStatus(String definitionStatus) { this.definitionStatus = definitionStatus; }
    public String getCurrent() { return current; }
    public void setCurrent(String current) { this.current = current; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public List<TicketWorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<TicketWorkflowNode> nodes) { this.nodes = nodes; }
    public List<TicketWorkflowTransition> getTransitions() { return transitions; }
    public void setTransitions(List<TicketWorkflowTransition> transitions) { this.transitions = transitions; }
}
