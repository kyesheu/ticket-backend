package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单流程连线请求参数
 *
 * @author ticket
 */
public class TicketWorkflowTransitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "来源节点不能为空")
    private String sourceNodeKey;

    @NotBlank(message = "目标节点不能为空")
    private String targetNodeKey;

    private String conditionField;
    private String conditionOperator;
    private String conditionValue;
    private String defaultTransition;
    private Integer sortOrder;

    public String getSourceNodeKey() { return sourceNodeKey; }
    public void setSourceNodeKey(String sourceNodeKey) { this.sourceNodeKey = sourceNodeKey; }
    public String getTargetNodeKey() { return targetNodeKey; }
    public void setTargetNodeKey(String targetNodeKey) { this.targetNodeKey = targetNodeKey; }
    public String getConditionField() { return conditionField; }
    public void setConditionField(String conditionField) { this.conditionField = conditionField; }
    public String getConditionOperator() { return conditionOperator; }
    public void setConditionOperator(String conditionOperator) { this.conditionOperator = conditionOperator; }
    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue; }
    public String getDefaultTransition() { return defaultTransition; }
    public void setDefaultTransition(String defaultTransition) { this.defaultTransition = defaultTransition; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
