package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketWorkflowInstance;
import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTask;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import com.ruoyi.ticket.dto.TicketWorkflowTaskActionDTO;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowAssigneeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowInstanceMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowNodeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTaskMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTransitionMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单流程任务动作测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单流程任务动作测试")
class TicketWorkflowEngineActionTest {

    @Mock private TicketCategoryMapper categoryMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketWorkflowDefinitionMapper definitionMapper;
    @Mock private TicketWorkflowNodeMapper nodeMapper;
    @Mock private TicketWorkflowTransitionMapper transitionMapper;
    @Mock private TicketWorkflowInstanceMapper instanceMapper;
    @Mock private TicketWorkflowTaskMapper taskMapper;
    @Mock private TicketWorkflowAssigneeMapper assigneeMapper;
    @Mock private TicketOperationLogMapper operationLogMapper;
    @Mock private ITicketNotificationService notificationService;
    @Mock private ITicketAccessPolicy accessPolicy;
    @InjectMocks private TicketWorkflowEngineImpl engine;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
    }

    @AfterEach
    void tearDown() { securityUtilsMock.close(); }

    @Test
    @DisplayName("分派任务完成后应更新工单并生成处理任务")
    void shouldCompleteAssignTaskAndCreateProcessTask() {
        mockRunningTask("ASSIGN", "ROLE", null, 1L);
        when(assigneeMapper.countUserEnabledRole(1L, 1L)).thenReturn(1);
        when(ticketMapper.checkUserExists(9L)).thenReturn(1);
        when(taskMapper.completePendingTask(any())).thenReturn(1);
        when(instanceMapper.updateInstance(any())).thenReturn(1);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("ASSIGN", "ASSIGN", "ROLE"), node("PROCESS", "PROCESS", "TICKET_ASSIGNEE")));
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(List.of(line("ASSIGN", "PROCESS")));
        TicketWorkflowTaskActionDTO dto = new TicketWorkflowTaskActionDTO(); dto.setAssigneeId(9L); dto.setComment("assign");

        engine.completeTask(10L, dto);

        verify(ticketMapper).updateTicket(any(Ticket.class));
        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("任务已被处理时重复提交应拒绝且不推进")
    void shouldRejectDuplicateCompletion() {
        mockRunningTask("ASSIGN", "ROLE", null, 1L);
        when(assigneeMapper.countUserEnabledRole(1L, 1L)).thenReturn(1);
        when(ticketMapper.checkUserExists(9L)).thenReturn(1);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("ASSIGN", "ASSIGN", "ROLE")));
        when(taskMapper.completePendingTask(any())).thenReturn(0);
        TicketWorkflowTaskActionDTO dto = new TicketWorkflowTaskActionDTO(); dto.setAssigneeId(9L);

        assertThatThrownBy(() -> engine.completeTask(10L, dto))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已处理");
        verify(taskMapper, never()).insertTask(any());
    }

    @Test
    @DisplayName("非任务候选人不能处理任务")
    void shouldRejectUnauthorizedActor() {
        mockRunningTask("PROCESS", "USER", 8L, 8L);

        assertThatThrownBy(() -> engine.completeTask(10L, new TicketWorkflowTaskActionDTO()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("处理人");
        verify(taskMapper, never()).completePendingTask(any());
    }

    @Test
    @DisplayName("首个人工节点不能退回")
    void shouldRejectReturnWithoutPreviousTask() {
        mockRunningTask("ASSIGN", "ROLE", null, 1L);
        when(assigneeMapper.countUserEnabledRole(1L, 1L)).thenReturn(1);
        when(taskMapper.selectPreviousCompletedTask(20L, 10L)).thenReturn(null);

        assertThatThrownBy(() -> engine.returnTask(10L, new TicketWorkflowTaskActionDTO()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("上一");
    }

    @Test
    @DisplayName("非创建人不能取消实例")
    void shouldRejectCancellationByNonCreator() {
        Ticket ticket = ticket(); ticket.setCreatorId(7L);
        when(instanceMapper.selectInstanceByTicketId(100L)).thenReturn(instance());
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket);

        assertThatThrownBy(() -> engine.cancelInstance(100L, "cancel"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("创建人");
    }

    @Test
    @DisplayName("非管理员不能终止实例")
    void shouldRejectTerminationByNonAdmin() {
        assertThatThrownBy(() -> engine.terminateInstance(100L, "terminate"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("管理员");
    }

    @Test
    @DisplayName("处理任务完成后应进入创建人确认节点")
    void shouldCompleteProcessTaskAndCreateConfirmTask() {
        mockRunningTask("PROCESS", "USER", 1L, 1L);
        Ticket ticket = ticket(); ticket.setStatus("PROCESSING"); ticket.setAssigneeId(1L);
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket);
        when(taskMapper.completePendingTask(any())).thenReturn(1);
        when(instanceMapper.updateInstance(any())).thenReturn(1);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("PROCESS", "PROCESS", "TICKET_ASSIGNEE"),
                node("CONFIRM", "CONFIRM", "TICKET_CREATOR")));
        when(transitionMapper.selectTransitionListByDefinitionId(1L))
                .thenReturn(List.of(line("PROCESS", "CONFIRM")));

        engine.completeTask(10L, new TicketWorkflowTaskActionDTO());

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).updateTicket(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("WAIT_CONFIRM");
        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getNodeKey()).isEqualTo("CONFIRM");
        assertThat(taskCaptor.getValue().getResolvedAssigneeId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("合法退回应重新生成上一节点任务")
    void shouldReturnToPreviousCompletedTask() {
        mockRunningTask("PROCESS", "USER", 1L, 1L);
        TicketWorkflowTask previous = new TicketWorkflowTask(); previous.setTaskId(5L); previous.setInstanceId(20L);
        previous.setNodeKey("ASSIGN"); previous.setNodeName("Assign"); previous.setTaskStatus("COMPLETED");
        previous.setAssigneeType("ROLE"); previous.setAssigneeRefId(1L);
        when(taskMapper.selectPreviousCompletedTask(20L, 10L)).thenReturn(previous);
        when(taskMapper.completePendingTask(any())).thenReturn(1);
        when(instanceMapper.updateInstance(any())).thenReturn(1);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("ASSIGN", "ASSIGN", "ROLE"),
                node("PROCESS", "PROCESS", "TICKET_ASSIGNEE")));

        engine.returnTask(10L, new TicketWorkflowTaskActionDTO());

        ArgumentCaptor<TicketWorkflowTask> taskCaptor = ArgumentCaptor.forClass(TicketWorkflowTask.class);
        verify(taskMapper).insertTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getNodeKey()).isEqualTo("ASSIGN");
        assertThat(taskCaptor.getValue().getTaskStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("创建人取消应结束实例并关闭待办")
    void shouldCancelRunningInstanceByCreator() {
        when(instanceMapper.selectInstanceByTicketId(100L)).thenReturn(instance());
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket());
        when(instanceMapper.updateInstance(any())).thenReturn(1);

        engine.cancelInstance(100L, "cancel reason");

        verify(taskMapper).closePendingTasks(any(), org.mockito.ArgumentMatchers.eq("CANCELLED"), any(), any());
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).updateTicket(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("管理员终止应结束实例并关闭待办")
    void shouldTerminateRunningInstanceByAdmin() {
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
        when(instanceMapper.selectInstanceByTicketId(100L)).thenReturn(instance());
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket());
        when(instanceMapper.updateInstance(any())).thenReturn(1);

        engine.terminateInstance(100L, "terminate reason");

        verify(taskMapper).closePendingTasks(any(), org.mockito.ArgumentMatchers.eq("TERMINATED"), any(), any());
        ArgumentCaptor<TicketWorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(TicketWorkflowInstance.class);
        verify(instanceMapper).updateInstance(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getWorkflowStatus()).isEqualTo("TERMINATED");
    }

    private void mockRunningTask(String nodeKey, String assigneeType, Long resolvedUser, Long referenceId) {
        TicketWorkflowTask task = new TicketWorkflowTask(); task.setTaskId(10L); task.setInstanceId(20L);
        task.setNodeKey(nodeKey); task.setNodeName(nodeKey); task.setTaskStatus("PENDING");
        task.setAssigneeType(assigneeType); task.setResolvedAssigneeId(resolvedUser); task.setAssigneeRefId(referenceId);
        when(taskMapper.selectTaskById(10L)).thenReturn(task);
        when(instanceMapper.selectInstanceById(20L)).thenReturn(instance());
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket());
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node(nodeKey, nodeKey, assigneeType)));
    }

    private TicketWorkflowInstance instance() {
        TicketWorkflowInstance instance = new TicketWorkflowInstance(); instance.setInstanceId(20L);
        instance.setTicketId(100L); instance.setDefinitionId(1L); instance.setWorkflowStatus("RUNNING"); return instance;
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket(); ticket.setTicketId(100L); ticket.setCreatorId(1L);
        ticket.setDeptId(3L); ticket.setPriority("HIGH"); ticket.setStatus("NEW"); return ticket;
    }

    private TicketWorkflowNode node(String key, String type, String assigneeType) {
        TicketWorkflowNode node = new TicketWorkflowNode(); node.setNodeKey(key); node.setNodeName(key);
        node.setNodeType(type); node.setAssigneeType(assigneeType); return node;
    }

    private TicketWorkflowTransition line(String source, String target) {
        TicketWorkflowTransition line = new TicketWorkflowTransition(); line.setSourceNodeKey(source);
        line.setTargetNodeKey(target); line.setDefaultTransition("1"); return line;
    }
}
