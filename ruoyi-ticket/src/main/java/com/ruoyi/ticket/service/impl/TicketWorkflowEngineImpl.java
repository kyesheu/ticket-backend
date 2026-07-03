package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketWorkflowDefinition;
import com.ruoyi.ticket.domain.TicketWorkflowInstance;
import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTask;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import com.ruoyi.ticket.enums.TicketWorkflowAssigneeType;
import com.ruoyi.ticket.enums.TicketWorkflowConditionField;
import com.ruoyi.ticket.enums.TicketWorkflowConditionOperator;
import com.ruoyi.ticket.enums.TicketWorkflowInstanceStatus;
import com.ruoyi.ticket.enums.TicketWorkflowNodeType;
import com.ruoyi.ticket.enums.TicketWorkflowTaskStatus;
import com.ruoyi.ticket.enums.TicketOperationType;
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.dto.TicketWorkflowTaskActionDTO;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowAssigneeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowInstanceMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowNodeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTaskMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTransitionMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.service.ITicketWorkflowEngine;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketNotificationService;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import com.ruoyi.ticket.enums.TicketCustomFieldType;
import com.ruoyi.ticket.mapper.TicketCustomFieldValueMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * 工单流程引擎实现
 *
 * @author ticket
 */
@Service
public class TicketWorkflowEngineImpl implements ITicketWorkflowEngine {

    private static final String STANDARD_WORKFLOW_KEY = "STANDARD";
    private static final String DEFAULT_TRANSITION = "1";

    @Autowired private TicketCategoryMapper categoryMapper;
    @Autowired private TicketMapper ticketMapper;
    @Autowired private TicketWorkflowDefinitionMapper definitionMapper;
    @Autowired private TicketWorkflowNodeMapper nodeMapper;
    @Autowired private TicketWorkflowTransitionMapper transitionMapper;
    @Autowired private TicketWorkflowInstanceMapper instanceMapper;
    @Autowired private TicketWorkflowTaskMapper taskMapper;
    @Autowired private TicketWorkflowAssigneeMapper assigneeMapper;
    @Autowired private TicketOperationLogMapper operationLogMapper;
    @Autowired private ITicketNotificationService notificationService;
    @Autowired private ITicketAccessPolicy accessPolicy;
    @Autowired private TicketCustomFieldValueMapper customFieldValueMapper;
    @Autowired private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startInstance(Ticket ticket) {
        TicketCategory category = categoryMapper.selectCategoryById(ticket.getCategoryId());
        if (category == null) {
            throw new ServiceException("工单分类不存在");
        }
        String workflowKey = category.getWorkflowKey() == null || category.getWorkflowKey().isBlank()
                ? STANDARD_WORKFLOW_KEY : category.getWorkflowKey();
        TicketWorkflowDefinition definition = definitionMapper.selectCurrentDefinitionByKey(workflowKey);
        if (definition == null) {
            throw new ServiceException("工单分类没有可用的已发布流程");
        }

        List<TicketWorkflowNode> nodes = nodeMapper.selectNodeListByDefinitionId(definition.getDefinitionId());
        List<TicketWorkflowTransition> transitions = transitionMapper
                .selectTransitionListByDefinitionId(definition.getDefinitionId());
        Map<String, TicketWorkflowNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TicketWorkflowNode::getNodeKey, Function.identity()));
        TicketWorkflowNode start = nodes.stream()
                .filter(node -> TicketWorkflowNodeType.START.name().equals(node.getNodeType()))
                .findFirst().orElseThrow(() -> new ServiceException("发布流程缺少 START 节点"));
        TicketWorkflowNode firstNode = selectNextNode(start.getNodeKey(), ticket, transitions, nodeMap);
        if (TicketWorkflowNodeType.END.name().equals(firstNode.getNodeType())) {
            throw new ServiceException("流程开始后必须先进入人工节点");
        }
        AssigneeSnapshot assignee = resolveAssignee(firstNode, ticket);

        Date now = new Date();
        TicketWorkflowInstance instance = new TicketWorkflowInstance();
        instance.setTicketId(ticket.getTicketId());
        instance.setDefinitionId(definition.getDefinitionId());
        instance.setWorkflowStatus(TicketWorkflowInstanceStatus.RUNNING.name());
        instance.setCurrentNodeKey(firstNode.getNodeKey());
        instance.setStartedAt(now);
        instance.setCreateTime(now);
        instance.setUpdateTime(now);
        instanceMapper.insertInstance(instance);

        TicketWorkflowTask task = new TicketWorkflowTask();
        task.setInstanceId(instance.getInstanceId());
        task.setNodeKey(firstNode.getNodeKey());
        task.setNodeName(firstNode.getNodeName());
        task.setTaskStatus(TicketWorkflowTaskStatus.PENDING.name());
        task.setAssigneeType(firstNode.getAssigneeType());
        task.setAssigneeRefId(assignee.referenceId());
        task.setResolvedAssigneeId(assignee.userId());
        task.setCreatedAt(now);
        taskMapper.insertTask(task);
        return instance.getInstanceId();
    }

    private TicketWorkflowNode selectNextNode(String sourceNodeKey, Ticket ticket,
                                              List<TicketWorkflowTransition> transitions,
                                              Map<String, TicketWorkflowNode> nodeMap) {
        TicketWorkflowTransition defaultLine = null;
        for (TicketWorkflowTransition line : transitions) {
            if (!sourceNodeKey.equals(line.getSourceNodeKey())) continue;
            if (DEFAULT_TRANSITION.equals(line.getDefaultTransition())) {
                defaultLine = line;
            } else if (matches(line, ticket)) {
                return requireTarget(line, nodeMap);
            }
        }
        if (defaultLine == null) throw new ServiceException("当前节点没有可匹配的流程分支");
        return requireTarget(defaultLine, nodeMap);
    }

    private TicketWorkflowNode requireTarget(TicketWorkflowTransition line, Map<String, TicketWorkflowNode> nodeMap) {
        TicketWorkflowNode target = nodeMap.get(line.getTargetNodeKey());
        if (target == null) throw new ServiceException("流程分支目标节点不存在");
        return target;
    }

    private boolean matches(TicketWorkflowTransition line, Ticket ticket) {
        TicketWorkflowConditionField field = TicketWorkflowConditionField.valueOf(line.getConditionField());
        if (field == TicketWorkflowConditionField.CUSTOM_FIELD) {
            return matchesCustomField(line, ticket.getTicketId());
        }
        String actual = switch (field) {
            case PRIORITY -> ticket.getPriority();
            case CATEGORY -> String.valueOf(ticket.getCategoryId());
            case CREATOR_DEPT -> String.valueOf(ticket.getDeptId());
            case CUSTOM_FIELD -> null;
        };
        TicketWorkflowConditionOperator operator = TicketWorkflowConditionOperator.valueOf(line.getConditionOperator());
        if (operator == TicketWorkflowConditionOperator.EQ) return line.getConditionValue().equals(actual);
        return List.of(line.getConditionValue().split(",")).stream().map(String::trim).anyMatch(actual::equals);
    }

    private boolean matchesCustomField(TicketWorkflowTransition line, Long ticketId) {
        if (line.getConditionKey() == null) {
            return false;
        }
        TicketCustomFieldValue value = customFieldValueMapper.selectByTicketAndKey(ticketId, line.getConditionKey());
        if (value == null || value.getNormalizedValue() == null || value.getFieldTypeSnapshot() == null) {
            return false;
        }
        TicketCustomFieldType fieldType;
        TicketWorkflowConditionOperator operator;
        try {
            fieldType = TicketCustomFieldType.valueOf(value.getFieldTypeSnapshot());
            operator = TicketWorkflowConditionOperator.valueOf(line.getConditionOperator());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (fieldType == TicketCustomFieldType.MULTI_SELECT) {
            return operator == TicketWorkflowConditionOperator.IN && matchesMultiSelect(line, value.getNormalizedValue());
        }
        if (operator == TicketWorkflowConditionOperator.EQ) {
            return line.getConditionValue().equals(value.getNormalizedValue());
        }
        Set<String> expectedValues = List.of(line.getConditionValue().split(",")).stream()
                .map(String::trim).collect(Collectors.toSet());
        return expectedValues.contains(value.getNormalizedValue());
    }

    private boolean matchesMultiSelect(TicketWorkflowTransition line, String normalizedValue) {
        try {
            Set<String> actualValues = Set.copyOf(objectMapper.readValue(normalizedValue, List.class).stream()
                    .map(String::valueOf).toList());
            return List.of(line.getConditionValue().split(",")).stream()
                    .map(String::trim).anyMatch(actualValues::contains);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return false;
        }
    }

    private AssigneeSnapshot resolveAssignee(TicketWorkflowNode node, Ticket ticket) {
        TicketWorkflowAssigneeType type = TicketWorkflowAssigneeType.valueOf(node.getAssigneeType());
        return switch (type) {
            case USER -> resolveUser(node.getAssigneeValue());
            case ROLE -> resolveRole(node.getAssigneeValue());
            case CREATOR_DEPT_LEADER -> resolveRequiredUser(
                    assigneeMapper.selectDepartmentLeaderUserId(ticket.getDeptId()), "创建人部门未配置有效负责人");
            case TICKET_ASSIGNEE -> resolveRequiredUser(ticket.getAssigneeId(), "工单尚未设置指派人");
            case TICKET_CREATOR -> resolveRequiredUser(ticket.getCreatorId(), "工单创建人不存在");
        };
    }

    private AssigneeSnapshot resolveUser(Long userId) {
        if (userId == null || ticketMapper.checkUserExists(userId) == 0) {
            throw new ServiceException("流程节点指定用户不存在");
        }
        return new AssigneeSnapshot(userId, userId);
    }

    private AssigneeSnapshot resolveRole(Long roleId) {
        if (roleId == null || assigneeMapper.countEnabledRoleById(roleId) == 0) {
            throw new ServiceException("流程节点指定角色不存在或已停用");
        }
        return new AssigneeSnapshot(roleId, null);
    }

    private AssigneeSnapshot resolveRequiredUser(Long userId, String message) {
        if (userId == null) throw new ServiceException(message);
        return new AssigneeSnapshot(userId, userId);
    }

    private record AssigneeSnapshot(Long referenceId, Long userId) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, TicketWorkflowTaskActionDTO dto) {
        TaskContext context = requireTaskContext(taskId);
        assertActor(context.task());
        TicketWorkflowNode currentNode = requireNode(context.instance().getDefinitionId(), context.task().getNodeKey());
        prepareTicketForCompletion(context.ticket(), currentNode, dto);

        Date now = new Date();
        context.task().setTaskStatus(TicketWorkflowTaskStatus.COMPLETED.name());
        context.task().setActionType("COMPLETE");
        context.task().setComment(dto.getComment());
        context.task().setCompletedBy(SecurityUtils.getUserId());
        context.task().setCompletedAt(now);
        if (taskMapper.completePendingTask(context.task()) != 1) {
            throw new ServiceException("流程任务已处理，请勿重复提交");
        }

        ticketMapper.updateTicket(context.ticket());
        advanceInstance(context, currentNode, now);
        saveActionLog(context.ticket(), currentNode, TicketOperationType.valueOf(currentNode.getNodeType()), dto.getComment());
        notifyCompletion(context.ticket(), currentNode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCurrentTask(Long ticketId, TicketWorkflowTaskActionDTO dto) {
        TicketWorkflowInstance instance = instanceMapper.selectInstanceByTicketId(ticketId);
        if (instance == null || !TicketWorkflowInstanceStatus.RUNNING.name().equals(instance.getWorkflowStatus())) {
            throw new ServiceException("流程实例不存在或已结束");
        }
        TicketWorkflowTask task = taskMapper.selectPendingTaskByInstanceId(instance.getInstanceId());
        if (task == null) throw new ServiceException("流程当前没有待处理任务");
        completeTask(task.getTaskId(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long taskId, TicketWorkflowTaskActionDTO dto) {
        TaskContext context = requireTaskContext(taskId);
        assertActor(context.task());
        TicketWorkflowTask previous = taskMapper.selectPreviousCompletedTask(
                context.instance().getInstanceId(), context.task().getTaskId());
        if (previous == null) throw new ServiceException("当前任务没有可退回的上一人工节点");

        Date now = new Date();
        context.task().setTaskStatus(TicketWorkflowTaskStatus.RETURNED.name());
        context.task().setActionType("RETURN"); context.task().setComment(dto.getComment());
        context.task().setCompletedBy(SecurityUtils.getUserId()); context.task().setCompletedAt(now);
        if (taskMapper.completePendingTask(context.task()) != 1) {
            throw new ServiceException("流程任务已处理，请勿重复提交");
        }
        createReturnedTask(previous, now);
        context.instance().setCurrentNodeKey(previous.getNodeKey()); context.instance().setUpdateTime(now);
        instanceMapper.updateInstance(context.instance());
        applyStatusForNode(context.ticket(), requireNode(context.instance().getDefinitionId(), previous.getNodeKey()), now);
        ticketMapper.updateTicket(context.ticket());
        saveActionLog(context.ticket(), requireNode(context.instance().getDefinitionId(), previous.getNodeKey()),
                TicketOperationType.RETURN, dto.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInstance(Long ticketId, String comment) {
        TaskContext context = requireRunningInstance(ticketId, "ticket:ticket:cancel");
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(context.ticket().getCreatorId())) {
            throw new ServiceException("您不是工单创建人，无法取消流程");
        }
        if (StringUtils.isBlank(comment)) throw new ServiceException("取消原因不能为空");
        finishInstance(context, TicketWorkflowInstanceStatus.CANCELLED,
                TicketWorkflowTaskStatus.CANCELLED, TicketOperationType.CANCEL, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateInstance(Long ticketId, String comment) {
        if (!SecurityUtils.isAdmin()) throw new ServiceException("只有管理员可以终止流程");
        TaskContext context = requireRunningInstance(ticketId, "ticket:workflow:publish");
        if (StringUtils.isBlank(comment)) throw new ServiceException("终止原因不能为空");
        finishInstance(context, TicketWorkflowInstanceStatus.TERMINATED,
                TicketWorkflowTaskStatus.TERMINATED, TicketOperationType.TERMINATE, comment);
    }

    private TaskContext requireTaskContext(Long taskId) {
        TicketWorkflowTask task = taskMapper.selectTaskById(taskId);
        if (task == null || !TicketWorkflowTaskStatus.PENDING.name().equals(task.getTaskStatus())) {
            throw new ServiceException("流程任务不存在或已处理");
        }
        TicketWorkflowInstance instance = instanceMapper.selectInstanceById(task.getInstanceId());
        if (instance == null || !TicketWorkflowInstanceStatus.RUNNING.name().equals(instance.getWorkflowStatus())) {
            throw new ServiceException("流程实例不存在或已结束");
        }
        accessPolicy.assertCanAccess(instance.getTicketId(), permissionForTask(instance, task));
        Ticket ticket = ticketMapper.selectTicketEntityById(instance.getTicketId());
        if (ticket == null) throw new ServiceException("工单不存在");
        return new TaskContext(task, instance, ticket);
    }

    private String permissionForTask(TicketWorkflowInstance instance, TicketWorkflowTask task) {
        TicketWorkflowNode node = requireNode(instance.getDefinitionId(), task.getNodeKey());
        TicketWorkflowNodeType type = TicketWorkflowNodeType.valueOf(node.getNodeType());
        return switch (type) {
            case ASSIGN -> "ticket:ticket:assign";
            case PROCESS -> "ticket:ticket:process";
            case CONFIRM -> "ticket:ticket:confirm";
            default -> "ticket:workflow:task";
        };
    }

    private TaskContext requireRunningInstance(Long ticketId, String permission) {
        accessPolicy.assertCanAccess(ticketId, permission);
        TicketWorkflowInstance instance = instanceMapper.selectInstanceByTicketId(ticketId);
        Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
        if (instance == null || ticket == null
                || !TicketWorkflowInstanceStatus.RUNNING.name().equals(instance.getWorkflowStatus())) {
            throw new ServiceException("流程实例不存在或已结束");
        }
        return new TaskContext(null, instance, ticket);
    }

    private void assertActor(TicketWorkflowTask task) {
        Long userId = SecurityUtils.getUserId();
        if (task.getResolvedAssigneeId() != null) {
            if (!userId.equals(task.getResolvedAssigneeId())) throw new ServiceException("当前用户不是任务处理人");
            return;
        }
        if (!TicketWorkflowAssigneeType.ROLE.name().equals(task.getAssigneeType())
                || assigneeMapper.countUserEnabledRole(userId, task.getAssigneeRefId()) == 0) {
            throw new ServiceException("当前用户不是任务处理人");
        }
    }

    private TicketWorkflowNode requireNode(Long definitionId, String nodeKey) {
        return nodeMapper.selectNodeListByDefinitionId(definitionId).stream()
                .filter(node -> nodeKey.equals(node.getNodeKey())).findFirst()
                .orElseThrow(() -> new ServiceException("流程节点不存在"));
    }

    private void prepareTicketForCompletion(Ticket ticket, TicketWorkflowNode node,
                                            TicketWorkflowTaskActionDTO dto) {
        Date now = new Date();
        if (TicketWorkflowNodeType.ASSIGN.name().equals(node.getNodeType())) {
            if (dto.getAssigneeId() == null || ticketMapper.checkUserExists(dto.getAssigneeId()) == 0) {
                throw new ServiceException("指派人不存在");
            }
            ticket.setAssigneeId(dto.getAssigneeId()); ticket.setProcessedAt(now);
        }
        applyStatusForNode(ticket, node, now);
        ticket.setUpdateBy(SecurityUtils.getUsername()); ticket.setUpdateTime(now);
    }

    private void applyStatusForNode(Ticket ticket, TicketWorkflowNode node, Date now) {
        TicketWorkflowNodeType type = TicketWorkflowNodeType.valueOf(node.getNodeType());
        switch (type) {
            case ASSIGN -> ticket.setStatus(TicketStatus.PROCESSING.name());
            case PROCESS -> ticket.setStatus(TicketStatus.WAIT_CONFIRM.name());
            case CONFIRM -> { ticket.setStatus(TicketStatus.CLOSED.name()); ticket.setClosedAt(now); }
            default -> { }
        }
    }

    private void advanceInstance(TaskContext context, TicketWorkflowNode currentNode, Date now) {
        List<TicketWorkflowNode> nodes = nodeMapper.selectNodeListByDefinitionId(context.instance().getDefinitionId());
        Map<String, TicketWorkflowNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TicketWorkflowNode::getNodeKey, Function.identity()));
        TicketWorkflowNode next = selectNextNode(currentNode.getNodeKey(), context.ticket(),
                transitionMapper.selectTransitionListByDefinitionId(context.instance().getDefinitionId()), nodeMap);
        if (TicketWorkflowNodeType.END.name().equals(next.getNodeType())) {
            context.instance().setWorkflowStatus(TicketWorkflowInstanceStatus.COMPLETED.name());
            context.instance().setCurrentNodeKey(next.getNodeKey()); context.instance().setEndedAt(now);
        } else {
            context.instance().setCurrentNodeKey(next.getNodeKey());
            createTask(context.instance().getInstanceId(), next, resolveAssignee(next, context.ticket()), now);
        }
        context.instance().setUpdateTime(now);
        if (instanceMapper.updateInstance(context.instance()) != 1) throw new ServiceException("流程实例状态已变化");
    }

    private void createTask(Long instanceId, TicketWorkflowNode node, AssigneeSnapshot assignee, Date now) {
        TicketWorkflowTask task = new TicketWorkflowTask(); task.setInstanceId(instanceId);
        task.setNodeKey(node.getNodeKey()); task.setNodeName(node.getNodeName());
        task.setTaskStatus(TicketWorkflowTaskStatus.PENDING.name()); task.setAssigneeType(node.getAssigneeType());
        task.setAssigneeRefId(assignee.referenceId()); task.setResolvedAssigneeId(assignee.userId());
        task.setCreatedAt(now); taskMapper.insertTask(task);
    }

    private void createReturnedTask(TicketWorkflowTask previous, Date now) {
        TicketWorkflowTask task = new TicketWorkflowTask(); task.setInstanceId(previous.getInstanceId());
        task.setNodeKey(previous.getNodeKey()); task.setNodeName(previous.getNodeName());
        task.setTaskStatus(TicketWorkflowTaskStatus.PENDING.name()); task.setAssigneeType(previous.getAssigneeType());
        task.setAssigneeRefId(previous.getAssigneeRefId()); task.setResolvedAssigneeId(previous.getResolvedAssigneeId());
        task.setCreatedAt(now); taskMapper.insertTask(task);
    }

    private void finishInstance(TaskContext context, TicketWorkflowInstanceStatus instanceStatus,
                                TicketWorkflowTaskStatus taskStatus, TicketOperationType operationType,
                                String comment) {
        Date now = new Date();
        taskMapper.closePendingTasks(context.instance().getInstanceId(), taskStatus.name(),
                SecurityUtils.getUserId(), now);
        context.instance().setWorkflowStatus(instanceStatus.name()); context.instance().setEndedAt(now);
        context.instance().setUpdateTime(now);
        if (instanceMapper.updateInstance(context.instance()) != 1) throw new ServiceException("流程实例状态已变化");
        context.ticket().setStatus(TicketStatus.CANCELLED.name()); context.ticket().setUpdateTime(now);
        context.ticket().setUpdateBy(SecurityUtils.getUsername()); ticketMapper.updateTicket(context.ticket());
        saveActionLog(context.ticket(), null, operationType, comment);
        Long recipient = context.ticket().getAssigneeId() == null
                ? context.ticket().getCreatorId() : context.ticket().getAssigneeId();
        notificationService.createNotification(context.ticket().getTicketId(), recipient,
                SecurityUtils.getUserId(), TicketNotificationType.CANCELLED,
                operationType.name() + ":" + context.ticket().getTicketId(), "工单流程已结束", comment);
    }

    private void saveActionLog(Ticket ticket, TicketWorkflowNode node,
                               TicketOperationType type, String comment) {
        TicketOperationLog log = new TicketOperationLog(); log.setTicketId(ticket.getTicketId());
        log.setOperationType(type.name()); log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername()); log.setToStatus(ticket.getStatus());
        log.setComment(comment); log.setOperateTime(new Date()); operationLogMapper.insertLog(log);
    }

    private void notifyCompletion(Ticket ticket, TicketWorkflowNode node) {
        TicketWorkflowNodeType type = TicketWorkflowNodeType.valueOf(node.getNodeType());
        if (type == TicketWorkflowNodeType.ASSIGN) {
            notificationService.createNotification(ticket.getTicketId(), ticket.getAssigneeId(),
                    SecurityUtils.getUserId(), TicketNotificationType.ASSIGNED,
                    "WF_ASSIGNED:" + ticket.getTicketId(), "工单已分派", "您收到一条新工单");
        } else if (type == TicketWorkflowNodeType.PROCESS) {
            notificationService.createNotification(ticket.getTicketId(), ticket.getCreatorId(),
                    SecurityUtils.getUserId(), TicketNotificationType.PROCESSED,
                    "WF_PROCESSED:" + ticket.getTicketId(), "工单处理完成", "工单等待确认");
        } else if (type == TicketWorkflowNodeType.CONFIRM && ticket.getAssigneeId() != null) {
            notificationService.createNotification(ticket.getTicketId(), ticket.getAssigneeId(),
                    SecurityUtils.getUserId(), TicketNotificationType.CLOSED,
                    "WF_CLOSED:" + ticket.getTicketId(), "工单已关闭", "工单已确认关闭");
        }
    }

    private record TaskContext(TicketWorkflowTask task, TicketWorkflowInstance instance, Ticket ticket) { }
}
