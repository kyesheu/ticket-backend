package com.ruoyi.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 工单流程定义请求参数
 *
 * @author ticket
 */
public class TicketWorkflowDefinitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "流程标识不能为空")
    private String workflowKey;

    @NotBlank(message = "流程名称不能为空")
    private String workflowName;

    private String remark;

    @Valid
    @NotEmpty(message = "流程节点不能为空")
    private List<TicketWorkflowNodeDTO> nodes;

    @Valid
    @NotEmpty(message = "流程连线不能为空")
    private List<TicketWorkflowTransitionDTO> transitions;

    public String getWorkflowKey() { return workflowKey; }
    public void setWorkflowKey(String workflowKey) { this.workflowKey = workflowKey; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<TicketWorkflowNodeDTO> getNodes() { return nodes; }
    public void setNodes(List<TicketWorkflowNodeDTO> nodes) { this.nodes = nodes; }
    public List<TicketWorkflowTransitionDTO> getTransitions() { return transitions; }
    public void setTransitions(List<TicketWorkflowTransitionDTO> transitions) { this.transitions = transitions; }
}
