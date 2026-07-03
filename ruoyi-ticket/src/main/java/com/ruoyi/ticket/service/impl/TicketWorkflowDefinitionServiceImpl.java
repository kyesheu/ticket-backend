package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketWorkflowDefinition;
import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import com.ruoyi.ticket.dto.TicketWorkflowBindDTO;
import com.ruoyi.ticket.dto.TicketWorkflowDefinitionDTO;
import com.ruoyi.ticket.dto.TicketWorkflowNodeDTO;
import com.ruoyi.ticket.dto.TicketWorkflowTransitionDTO;
import com.ruoyi.ticket.enums.TicketWorkflowAssigneeType;
import com.ruoyi.ticket.enums.TicketWorkflowConditionField;
import com.ruoyi.ticket.enums.TicketWorkflowConditionOperator;
import com.ruoyi.ticket.enums.TicketWorkflowDefinitionStatus;
import com.ruoyi.ticket.enums.TicketWorkflowNodeType;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowNodeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTransitionMapper;
import com.ruoyi.ticket.service.ITicketWorkflowDefinitionService;
import com.ruoyi.ticket.vo.TicketWorkflowDefinitionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单流程定义 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketWorkflowDefinitionServiceImpl implements ITicketWorkflowDefinitionService {

    private static final String YES = "1";
    private static final String NO = "0";

    @Autowired private TicketWorkflowDefinitionMapper definitionMapper;
    @Autowired private TicketWorkflowNodeMapper nodeMapper;
    @Autowired private TicketWorkflowTransitionMapper transitionMapper;
    @Autowired private TicketCategoryMapper categoryMapper;

    @Override
    public List<TicketWorkflowDefinitionVO> selectDefinitionList() {
        return definitionMapper.selectDefinitionList().stream().map(this::toVO).toList();
    }

    @Override
    public TicketWorkflowDefinitionVO selectDefinitionById(Long definitionId) {
        TicketWorkflowDefinition definition = requireDefinition(definitionId);
        TicketWorkflowDefinitionVO vo = toVO(definition);
        vo.setNodes(nodeMapper.selectNodeListByDefinitionId(definitionId));
        vo.setTransitions(transitionMapper.selectTransitionListByDefinitionId(definitionId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertDraft(TicketWorkflowDefinitionDTO dto) {
        TicketWorkflowDefinition latest = definitionMapper.selectLatestDefinitionByKey(dto.getWorkflowKey());
        int version = latest == null ? 1 : latest.getVersion() + 1;
        TicketWorkflowDefinition definition = createDefinition(dto, version);
        definitionMapper.insertDefinition(definition);
        saveGraph(definition.getDefinitionId(), dto.getNodes(), dto.getTransitions());
        return definition.getDefinitionId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDraft(Long definitionId, TicketWorkflowDefinitionDTO dto) {
        TicketWorkflowDefinition definition = requireDraft(definitionId);
        if (!definition.getWorkflowKey().equals(dto.getWorkflowKey())) {
            throw new ServiceException("流程标识不允许修改");
        }
        definition.setWorkflowName(dto.getWorkflowName()); definition.setRemark(dto.getRemark());
        definition.setUpdateBy(SecurityUtils.getUsername()); definition.setUpdateTime(new Date());
        int rows = definitionMapper.updateDefinition(definition);
        transitionMapper.deleteTransitionsByDefinitionId(definitionId);
        nodeMapper.deleteNodesByDefinitionId(definitionId);
        saveGraph(definitionId, dto.getNodes(), dto.getTransitions());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyVersion(Long definitionId) {
        TicketWorkflowDefinition source = requireDefinition(definitionId);
        TicketWorkflowDefinition latest = definitionMapper.selectLatestDefinitionByKey(source.getWorkflowKey());
        TicketWorkflowDefinition copy = new TicketWorkflowDefinition();
        BeanUtils.copyProperties(source, copy, "definitionId", "createTime", "updateTime");
        copy.setVersion(latest.getVersion() + 1); copy.setDefinitionStatus(TicketWorkflowDefinitionStatus.DRAFT.name());
        copy.setCurrent(NO); copy.setCreateBy(SecurityUtils.getUsername()); copy.setCreateTime(new Date());
        definitionMapper.insertDefinition(copy);
        saveGraph(copy.getDefinitionId(), toNodeDTOs(definitionId), toTransitionDTOs(definitionId));
        return copy.getDefinitionId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishDefinition(Long definitionId) {
        TicketWorkflowDefinition definition = requireDraft(definitionId);
        definitionMapper.selectLatestDefinitionByKeyForUpdate(definition.getWorkflowKey());
        validateGraph(nodeMapper.selectNodeListByDefinitionId(definitionId),
                transitionMapper.selectTransitionListByDefinitionId(definitionId));
        definitionMapper.clearCurrentByKey(definition.getWorkflowKey());
        definition.setDefinitionStatus(TicketWorkflowDefinitionStatus.PUBLISHED.name());
        definition.setCurrent(YES); definition.setUpdateBy(SecurityUtils.getUsername()); definition.setUpdateTime(new Date());
        int rows = definitionMapper.publishDefinition(definition);
        if (rows != 1) {
            throw new ServiceException("流程状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    public int bindCategory(TicketWorkflowBindDTO dto) {
        if (categoryMapper.selectCategoryById(dto.getCategoryId()) == null) throw new ServiceException("工单分类不存在");
        if (definitionMapper.selectCurrentDefinitionByKey(dto.getWorkflowKey()) == null) {
            throw new ServiceException("流程没有已发布版本");
        }
        return categoryMapper.updateWorkflowKey(dto.getCategoryId(), dto.getWorkflowKey(), SecurityUtils.getUsername());
    }

    private TicketWorkflowDefinition requireDefinition(Long id) {
        TicketWorkflowDefinition value = definitionMapper.selectDefinitionById(id);
        if (value == null) throw new ServiceException("流程定义不存在");
        return value;
    }

    private TicketWorkflowDefinition requireDraft(Long id) {
        TicketWorkflowDefinition value = requireDefinition(id);
        if (!TicketWorkflowDefinitionStatus.DRAFT.name().equals(value.getDefinitionStatus())) {
            throw new ServiceException("只有草稿流程可以执行该操作");
        }
        return value;
    }

    private TicketWorkflowDefinition createDefinition(TicketWorkflowDefinitionDTO dto, int version) {
        TicketWorkflowDefinition value = new TicketWorkflowDefinition();
        value.setWorkflowKey(dto.getWorkflowKey()); value.setWorkflowName(dto.getWorkflowName()); value.setVersion(version);
        value.setDefinitionStatus(TicketWorkflowDefinitionStatus.DRAFT.name()); value.setCurrent(NO);
        value.setRemark(dto.getRemark()); value.setCreateBy(SecurityUtils.getUsername()); value.setCreateTime(new Date());
        return value;
    }

    private void saveGraph(Long id, List<TicketWorkflowNodeDTO> nodes, List<TicketWorkflowTransitionDTO> transitions) {
        for (TicketWorkflowNodeDTO dto : nodes) {
            TicketWorkflowNode value = new TicketWorkflowNode(); BeanUtils.copyProperties(dto, value);
            value.setDefinitionId(id); nodeMapper.insertNode(value);
        }
        for (TicketWorkflowTransitionDTO dto : transitions) {
            TicketWorkflowTransition value = new TicketWorkflowTransition(); BeanUtils.copyProperties(dto, value);
            value.setDefinitionId(id); transitionMapper.insertTransition(value);
        }
    }

    private void validateGraph(List<TicketWorkflowNode> nodes, List<TicketWorkflowTransition> transitions) {
        Map<String, TicketWorkflowNode> byKey = new HashMap<>();
        for (TicketWorkflowNode node : nodes) {
            if (byKey.put(node.getNodeKey(), node) != null) throw new ServiceException("节点标识不能重复");
        }
        long starts = nodes.stream().filter(n -> TicketWorkflowNodeType.START.name().equals(n.getNodeType())).count();
        long ends = nodes.stream().filter(n -> TicketWorkflowNodeType.END.name().equals(n.getNodeType())).count();
        if (starts != 1) throw new ServiceException("流程必须且只能包含一个 START 节点");
        if (ends < 1) throw new ServiceException("流程至少包含一个 END 节点");
        for (TicketWorkflowNode node : nodes) validateAssignee(node);
        Map<String, List<TicketWorkflowTransition>> outgoing = transitions.stream()
                .collect(java.util.stream.Collectors.groupingBy(TicketWorkflowTransition::getSourceNodeKey));
        for (TicketWorkflowTransition transition : transitions) {
            if (!byKey.containsKey(transition.getSourceNodeKey()) || !byKey.containsKey(transition.getTargetNodeKey())) {
                throw new ServiceException("流程连线引用了不存在的节点");
            }
            validateCondition(transition);
        }
        for (TicketWorkflowNode node : nodes) {
            if (!TicketWorkflowNodeType.END.name().equals(node.getNodeType())) {
                List<TicketWorkflowTransition> lines = outgoing.getOrDefault(node.getNodeKey(), List.of());
                if (lines.stream().filter(t -> YES.equals(t.getDefaultTransition())).count() != 1) {
                    throw new ServiceException("每个非结束节点必须且只能有一条默认连线");
                }
            }
        }
        ensureReachable(nodes, transitions, byKey);
        ensureAcyclic(nodes, transitions);
    }

    private void validateCondition(TicketWorkflowTransition transition) {
        if (YES.equals(transition.getDefaultTransition())) {
            if (transition.getConditionField() != null || transition.getConditionOperator() != null
                    || transition.getConditionValue() != null) {
                throw new ServiceException("默认连线不能配置条件");
            }
            return;
        }
        try {
            TicketWorkflowConditionField.valueOf(transition.getConditionField());
            TicketWorkflowConditionOperator.valueOf(transition.getConditionOperator());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ServiceException("流程条件字段或运算符无效");
        }
        if (transition.getConditionValue() == null || transition.getConditionValue().isBlank()) {
            throw new ServiceException("流程条件值不能为空");
        }
    }

    private void validateAssignee(TicketWorkflowNode node) {
        if (TicketWorkflowNodeType.START.name().equals(node.getNodeType())
                || TicketWorkflowNodeType.END.name().equals(node.getNodeType())) return;
        try { TicketWorkflowAssigneeType type = TicketWorkflowAssigneeType.valueOf(node.getAssigneeType());
            if ((type == TicketWorkflowAssigneeType.USER || type == TicketWorkflowAssigneeType.ROLE)
                    && node.getAssigneeValue() == null) throw new ServiceException("人工节点处理人配置不完整");
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ServiceException("人工节点处理人类型无效");
        }
    }

    private void ensureReachable(List<TicketWorkflowNode> nodes, List<TicketWorkflowTransition> transitions,
                                 Map<String, TicketWorkflowNode> byKey) {
        String start = nodes.stream().filter(n -> TicketWorkflowNodeType.START.name().equals(n.getNodeType()))
                .findFirst().orElseThrow().getNodeKey();
        Set<String> reached = new HashSet<>(); ArrayDeque<String> queue = new ArrayDeque<>(); queue.add(start);
        while (!queue.isEmpty()) { String key = queue.remove(); if (!reached.add(key)) continue;
            transitions.stream().filter(t -> key.equals(t.getSourceNodeKey()))
                    .map(TicketWorkflowTransition::getTargetNodeKey).forEach(queue::add); }
        if (reached.size() != byKey.size()) throw new ServiceException("流程存在不可达节点");
    }

    private void ensureAcyclic(List<TicketWorkflowNode> nodes, List<TicketWorkflowTransition> transitions) {
        Map<String, Integer> indegrees = new HashMap<>();
        nodes.forEach(node -> indegrees.put(node.getNodeKey(), 0));
        transitions.forEach(line -> indegrees.computeIfPresent(
                line.getTargetNodeKey(), (key, value) -> value + 1));
        ArrayDeque<String> queue = new ArrayDeque<>();
        indegrees.forEach((key, value) -> { if (value == 0) queue.add(key); });
        int visited = 0;
        while (!queue.isEmpty()) {
            String key = queue.remove();
            visited++;
            for (TicketWorkflowTransition line : transitions) {
                if (!key.equals(line.getSourceNodeKey())) continue;
                int remaining = indegrees.computeIfPresent(
                        line.getTargetNodeKey(), (target, value) -> value - 1);
                if (remaining == 0) queue.add(line.getTargetNodeKey());
            }
        }
        if (visited != nodes.size()) throw new ServiceException("流程不允许存在环路");
    }

    private List<TicketWorkflowNodeDTO> toNodeDTOs(Long id) { return nodeMapper.selectNodeListByDefinitionId(id).stream().map(n -> {
        TicketWorkflowNodeDTO dto = new TicketWorkflowNodeDTO(); BeanUtils.copyProperties(n, dto); return dto; }).toList(); }
    private List<TicketWorkflowTransitionDTO> toTransitionDTOs(Long id) {
        return transitionMapper.selectTransitionListByDefinitionId(id).stream().map(t -> {
            TicketWorkflowTransitionDTO dto = new TicketWorkflowTransitionDTO(); BeanUtils.copyProperties(t, dto); return dto; }).toList(); }
    private TicketWorkflowDefinitionVO toVO(TicketWorkflowDefinition value) {
        TicketWorkflowDefinitionVO vo = new TicketWorkflowDefinitionVO(); BeanUtils.copyProperties(value, vo); return vo; }
}
