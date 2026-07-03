package com.ruoyi.ticket.domain;

import com.ruoyi.ticket.enums.TicketWorkflowAssigneeType;
import com.ruoyi.ticket.enums.TicketWorkflowConditionField;
import com.ruoyi.ticket.enums.TicketWorkflowConditionOperator;
import com.ruoyi.ticket.enums.TicketWorkflowDefinitionStatus;
import com.ruoyi.ticket.enums.TicketWorkflowNodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单流程基础模型测试
 *
 * @author ticket
 */
@DisplayName("工单流程基础模型测试")
class TicketWorkflowModelTest {

    @Test
    @DisplayName("流程实体均支持序列化")
    void shouldMakeWorkflowDomainsSerializable() {
        assertThat(new TicketWorkflowDefinition()).isInstanceOf(Serializable.class);
        assertThat(new TicketWorkflowNode()).isInstanceOf(Serializable.class);
        assertThat(new TicketWorkflowTransition()).isInstanceOf(Serializable.class);
        assertThat(new TicketWorkflowInstance()).isInstanceOf(Serializable.class);
        assertThat(new TicketWorkflowTask()).isInstanceOf(Serializable.class);
    }

    @Test
    @DisplayName("流程定义和节点固定值完整")
    void shouldContainDefinitionAndNodeValues() {
        assertThat(TicketWorkflowDefinitionStatus.values()).containsExactly(
                TicketWorkflowDefinitionStatus.DRAFT,
                TicketWorkflowDefinitionStatus.PUBLISHED,
                TicketWorkflowDefinitionStatus.DISABLED);
        assertThat(TicketWorkflowNodeType.values()).containsExactly(
                TicketWorkflowNodeType.START,
                TicketWorkflowNodeType.ASSIGN,
                TicketWorkflowNodeType.PROCESS,
                TicketWorkflowNodeType.CONFIRM,
                TicketWorkflowNodeType.END);
    }

    @Test
    @DisplayName("处理人和条件仅允许白名单值")
    void shouldContainOnlyWhitelistedAssignmentAndConditionValues() {
        assertThat(TicketWorkflowAssigneeType.values()).containsExactly(
                TicketWorkflowAssigneeType.USER,
                TicketWorkflowAssigneeType.ROLE,
                TicketWorkflowAssigneeType.CREATOR_DEPT_LEADER,
                TicketWorkflowAssigneeType.TICKET_ASSIGNEE,
                TicketWorkflowAssigneeType.TICKET_CREATOR);
        assertThat(TicketWorkflowConditionField.values()).containsExactly(
                TicketWorkflowConditionField.PRIORITY,
                TicketWorkflowConditionField.CATEGORY,
                TicketWorkflowConditionField.CREATOR_DEPT,
                TicketWorkflowConditionField.CUSTOM_FIELD);
        assertThat(TicketWorkflowConditionOperator.values()).containsExactly(
                TicketWorkflowConditionOperator.EQ,
                TicketWorkflowConditionOperator.IN);
    }
}
