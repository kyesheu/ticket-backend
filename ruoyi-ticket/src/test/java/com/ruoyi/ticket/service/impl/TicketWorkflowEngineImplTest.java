package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketWorkflowDefinition;
import com.ruoyi.ticket.domain.TicketWorkflowInstance;
import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTask;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowAssigneeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowInstanceMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowNodeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTaskMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTransitionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldValueMapper;
import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单流程引擎启动测试
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单流程引擎启动测试")
class TicketWorkflowEngineImplTest {

    @Mock private TicketCategoryMapper categoryMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketWorkflowDefinitionMapper definitionMapper;
    @Mock private TicketWorkflowNodeMapper nodeMapper;
    @Mock private TicketWorkflowTransitionMapper transitionMapper;
    @Mock private TicketWorkflowInstanceMapper instanceMapper;
    @Mock private TicketWorkflowTaskMapper taskMapper;
    @Mock private TicketWorkflowAssigneeMapper assigneeMapper;
    @Mock private TicketCustomFieldValueMapper customFieldValueMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private TicketWorkflowEngineImpl engine;

    @Test
    @DisplayName("分类未绑定时应锁定标准流程并创建角色任务")
    void shouldStartStandardWorkflowWithRoleTask() {
        Ticket ticket = ticket();
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        mockDefinition("STANDARD", List.of(node("START", "START", null, null),
                node("ASSIGN", "ASSIGN", "ROLE", 1L), node("END", "END", null, null)),
                List.of(line("START", "ASSIGN", null, null, null, "1"),
                        line("ASSIGN", "END", null, null, null, "1")));
        when(assigneeMapper.countEnabledRoleById(1L)).thenReturn(1);
        when(instanceMapper.insertInstance(any())).thenAnswer(invocation -> {
            invocation.<TicketWorkflowInstance>getArgument(0).setInstanceId(20L); return 1;
        });

        Long instanceId = engine.startInstance(ticket);

        assertThat(instanceId).isEqualTo(20L);
        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeRefId()).isEqualTo(1L);
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isNull();
    }

    @Test
    @DisplayName("优先级条件命中时应进入指定用户节点")
    void shouldRouteByPriorityToUser() {
        Ticket ticket = ticket(); ticket.setPriority("URGENT");
        TicketCategory category = new TicketCategory(); category.setWorkflowKey("CUSTOM");
        when(categoryMapper.selectCategoryById(6L)).thenReturn(category);
        mockDefinition("CUSTOM", List.of(node("START", "START", null, null),
                node("URGENT", "PROCESS", "USER", 9L), node("NORMAL", "PROCESS", "USER", 8L)),
                List.of(line("START", "URGENT", "PRIORITY", "EQ", "URGENT", "0"),
                        line("START", "NORMAL", null, null, null, "1")));
        when(ticketMapper.checkUserExists(9L)).thenReturn(1);
        when(instanceMapper.insertInstance(any())).thenAnswer(invocation -> {
            invocation.<TicketWorkflowInstance>getArgument(0).setInstanceId(21L); return 1;
        });

        engine.startInstance(ticket);

        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getNodeKey()).isEqualTo("URGENT");
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("自定义字段 EQ 命中时应进入条件节点")
    void shouldRouteByCustomFieldEquality() {
        Ticket ticket = ticket();
        TicketCategory category = new TicketCategory(); category.setWorkflowKey("CUSTOM");
        when(categoryMapper.selectCategoryById(6L)).thenReturn(category);
        TicketWorkflowTransition condition = line("START", "MATCH", "CUSTOM_FIELD", "EQ", "SHANGHAI", "0");
        condition.setConditionKey("LOCATION");
        mockDefinition("CUSTOM", List.of(node("START", "START", null, null),
                node("MATCH", "PROCESS", "USER", 9L), node("DEFAULT", "PROCESS", "USER", 8L)),
                List.of(condition, line("START", "DEFAULT", null, null, null, "1")));
        when(customFieldValueMapper.selectByTicketAndKey(100L, "LOCATION"))
                .thenReturn(customValue("TEXT", "SHANGHAI"));
        when(ticketMapper.checkUserExists(9L)).thenReturn(1);

        engine.startInstance(ticket);

        ArgumentCaptor<TicketWorkflowTask> captor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(captor.capture());
        assertThat(captor.getValue().getNodeKey()).isEqualTo("MATCH");
    }

    @Test
    @DisplayName("自定义多选字段 IN 命中任一选项时应进入条件节点")
    void shouldRouteByCustomMultiSelectMembership() {
        Ticket ticket = ticket();
        TicketCategory category = new TicketCategory(); category.setWorkflowKey("CUSTOM");
        when(categoryMapper.selectCategoryById(6L)).thenReturn(category);
        TicketWorkflowTransition condition = line("START", "MATCH", "CUSTOM_FIELD", "IN", "A,B", "0");
        condition.setConditionKey("TAGS");
        mockDefinition("CUSTOM", List.of(node("START", "START", null, null),
                node("MATCH", "PROCESS", "USER", 9L), node("DEFAULT", "PROCESS", "USER", 8L)),
                List.of(condition, line("START", "DEFAULT", null, null, null, "1")));
        when(customFieldValueMapper.selectByTicketAndKey(100L, "TAGS"))
                .thenReturn(customValue("MULTI_SELECT", "[\"B\",\"C\"]"));
        when(ticketMapper.checkUserExists(9L)).thenReturn(1);

        engine.startInstance(ticket);

        ArgumentCaptor<TicketWorkflowTask> captor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(captor.capture());
        assertThat(captor.getValue().getNodeKey()).isEqualTo("MATCH");
    }

    @Test
    @DisplayName("自定义字段缺失或类型不兼容时应走默认分支")
    void shouldUseDefaultForMissingOrIncompatibleCustomField() {
        Ticket ticket = ticket();
        TicketCategory category = new TicketCategory(); category.setWorkflowKey("CUSTOM");
        when(categoryMapper.selectCategoryById(6L)).thenReturn(category);
        TicketWorkflowTransition condition = line("START", "MATCH", "CUSTOM_FIELD", "EQ", "A", "0");
        condition.setConditionKey("TAGS");
        mockDefinition("CUSTOM", List.of(node("START", "START", null, null),
                node("MATCH", "PROCESS", "USER", 9L), node("DEFAULT", "PROCESS", "USER", 8L)),
                List.of(condition, line("START", "DEFAULT", null, null, null, "1")));
        when(customFieldValueMapper.selectByTicketAndKey(100L, "TAGS"))
                .thenReturn(customValue("MULTI_SELECT", "[\"A\"]"));
        when(ticketMapper.checkUserExists(8L)).thenReturn(1);

        engine.startInstance(ticket);

        ArgumentCaptor<TicketWorkflowTask> captor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(captor.capture());
        assertThat(captor.getValue().getNodeKey()).isEqualTo("DEFAULT");
    }

    @Test
    @DisplayName("部门负责人规则应固化负责人用户")
    void shouldResolveDepartmentLeader() {
        Ticket ticket = ticket();
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        mockDefinition("STANDARD", List.of(node("START", "START", null, null),
                node("LEADER", "PROCESS", "CREATOR_DEPT_LEADER", null)),
                List.of(line("START", "LEADER", null, null, null, "1")));
        when(assigneeMapper.selectDepartmentLeaderUserId(3L)).thenReturn(11L);
        when(instanceMapper.insertInstance(any())).thenAnswer(invocation -> {
            invocation.<TicketWorkflowInstance>getArgument(0).setInstanceId(22L); return 1;
        });

        engine.startInstance(ticket);

        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("无当前发布版本时应拒绝启动且不创建实例")
    void shouldRejectMissingPublishedDefinition() {
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        when(definitionMapper.selectCurrentDefinitionByKey("STANDARD")).thenReturn(null);

        assertThatThrownBy(() -> engine.startInstance(ticket()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("发布");
        verify(instanceMapper, never()).insertInstance(any());
    }

    @Test
    @DisplayName("工单指派人为空时不允许激活指派人节点")
    void shouldRejectMissingTicketAssignee() {
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        mockDefinition("STANDARD", List.of(node("START", "START", null, null),
                node("PROCESS", "PROCESS", "TICKET_ASSIGNEE", null)),
                List.of(line("START", "PROCESS", null, null, null, "1")));

        assertThatThrownBy(() -> engine.startInstance(ticket()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("指派人");
    }

    @Test
    @DisplayName("工单创建人规则应固化创建人")
    void shouldResolveTicketCreator() {
        Ticket ticket = ticket();
        mockSingleRuntimeAssignee(ticket, "TICKET_CREATOR", 23L);
        engine.startInstance(ticket);
        assertResolvedUser(7L);
    }

    @Test
    @DisplayName("工单指派人规则应固化当前指派人")
    void shouldResolveTicketAssignee() {
        Ticket ticket = ticket(); ticket.setAssigneeId(12L);
        mockSingleRuntimeAssignee(ticket, "TICKET_ASSIGNEE", 24L);
        engine.startInstance(ticket);
        assertResolvedUser(12L);
    }

    private void mockSingleRuntimeAssignee(Ticket ticket, String assigneeType, Long instanceId) {
        when(categoryMapper.selectCategoryById(ticket.getCategoryId())).thenReturn(new TicketCategory());
        mockDefinition("STANDARD", List.of(node("START", "START", null, null),
                node("WORK", "PROCESS", assigneeType, null)),
                List.of(line("START", "WORK", null, null, null, "1")));
        when(instanceMapper.insertInstance(any())).thenAnswer(invocation -> {
            invocation.<TicketWorkflowInstance>getArgument(0).setInstanceId(instanceId); return 1;
        });
    }

    private void assertResolvedUser(Long userId) {
        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isEqualTo(userId);
    }

    private void mockDefinition(String key, List<TicketWorkflowNode> nodes,
                                List<TicketWorkflowTransition> transitions) {
        TicketWorkflowDefinition definition = new TicketWorkflowDefinition();
        definition.setDefinitionId(1L); definition.setWorkflowKey(key); definition.setVersion(1);
        when(definitionMapper.selectCurrentDefinitionByKey(key)).thenReturn(definition);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(nodes);
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(transitions);
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket(); ticket.setTicketId(100L); ticket.setCategoryId(6L);
        ticket.setPriority("HIGH"); ticket.setCreatorId(7L); ticket.setDeptId(3L);
        return ticket;
    }

    private TicketWorkflowNode node(String key, String type, String assigneeType, Long value) {
        TicketWorkflowNode node = new TicketWorkflowNode(); node.setNodeKey(key); node.setNodeName(key);
        node.setNodeType(type); node.setAssigneeType(assigneeType); node.setAssigneeValue(value); return node;
    }

    private TicketWorkflowTransition line(String source, String target, String field,
                                          String operator, String value, String defaultLine) {
        TicketWorkflowTransition line = new TicketWorkflowTransition(); line.setSourceNodeKey(source);
        line.setTargetNodeKey(target); line.setConditionField(field); line.setConditionOperator(operator);
        line.setConditionValue(value); line.setDefaultTransition(defaultLine); return line;
    }

    private TicketCustomFieldValue customValue(String fieldType, String normalizedValue) {
        TicketCustomFieldValue value = new TicketCustomFieldValue();
        value.setFieldTypeSnapshot(fieldType); value.setNormalizedValue(normalizedValue); return value;
    }
}
