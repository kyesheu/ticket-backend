package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单流程Transition实体
 *
 * @author ticket
 */
public class TicketWorkflowTransition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 连线 ID */
    private Long transitionId;

    /** 流程定义 ID */
    private Long definitionId;

    /** 来源节点标识 */
    private String sourceNodeKey;

    /** 目标节点标识 */
    private String targetNodeKey;

    /** 条件字段 */
    private String conditionField;

    /** 条件运算符 */
    private String conditionOperator;

    /** 条件值 */
    private String conditionValue;

    /** 是否默认连线 */
    private String defaultTransition;

    /** 匹配顺序 */
    private Integer sortOrder;

    public Long getTransitionId() {
        return transitionId;
    }

    public void setTransitionId(Long transitionId) {
        this.transitionId = transitionId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getSourceNodeKey() {
        return sourceNodeKey;
    }

    public void setSourceNodeKey(String sourceNodeKey) {
        this.sourceNodeKey = sourceNodeKey;
    }

    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public void setTargetNodeKey(String targetNodeKey) {
        this.targetNodeKey = targetNodeKey;
    }

    public String getConditionField() {
        return conditionField;
    }

    public void setConditionField(String conditionField) {
        this.conditionField = conditionField;
    }

    public String getConditionOperator() {
        return conditionOperator;
    }

    public void setConditionOperator(String conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public String getDefaultTransition() {
        return defaultTransition;
    }

    public void setDefaultTransition(String defaultTransition) {
        this.defaultTransition = defaultTransition;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
