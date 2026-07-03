package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketWorkflowDefinition;
import com.ruoyi.ticket.domain.TicketWorkflowNode;
import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import com.ruoyi.ticket.dto.TicketWorkflowBindDTO;
import com.ruoyi.ticket.dto.TicketWorkflowDefinitionDTO;
import com.ruoyi.ticket.dto.TicketWorkflowNodeDTO;
import com.ruoyi.ticket.dto.TicketWorkflowTransitionDTO;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowNodeMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowTransitionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单流程定义 Service 单元测试
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单流程定义 Service 测试")
class TicketWorkflowDefinitionServiceImplTest {

    @Mock private TicketWorkflowDefinitionMapper definitionMapper;
    @Mock private TicketWorkflowNodeMapper nodeMapper;
    @Mock private TicketWorkflowTransitionMapper transitionMapper;
    @Mock private TicketCategoryMapper categoryMapper;
    @InjectMocks private TicketWorkflowDefinitionServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("创建合法草稿应保存定义节点和连线")
    void insertValidDraftShouldSaveWholeDefinition() {
        when(definitionMapper.selectLatestDefinitionByKey("LEAVE")).thenReturn(null);
        when(definitionMapper.insertDefinition(any())).thenAnswer(invocation -> {
            invocation.<TicketWorkflowDefinition>getArgument(0).setDefinitionId(10L);
            return 1;
        });

        service.insertDraft(createValidDefinitionDTO());

        verify(definitionMapper).insertDefinition(any(TicketWorkflowDefinition.class));
        verify(nodeMapper, org.mockito.Mockito.times(3)).insertNode(any(TicketWorkflowNode.class));
        verify(transitionMapper, org.mockito.Mockito.times(2))
                .insertTransition(any(TicketWorkflowTransition.class));
    }

    @Test
    @DisplayName("已发布定义不允许修改")
    void updatePublishedDefinitionShouldFail() {
        when(definitionMapper.selectDefinitionById(1L)).thenReturn(definition("PUBLISHED"));

        assertThatThrownBy(() -> service.updateDraft(1L, createValidDefinitionDTO()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("草稿");
        verify(definitionMapper, never()).updateDefinition(any());
    }

    @Test
    @DisplayName("缺少开始节点的流程不允许发布")
    void publishWithoutStartShouldFail() {
        when(definitionMapper.selectDefinitionById(1L)).thenReturn(definition("DRAFT"));
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(node("END", "END", null)));
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.publishDefinition(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("START");
    }

    @Test
    @DisplayName("人工节点未配置处理人不允许发布")
    void publishWithoutAssigneeShouldFail() {
        when(definitionMapper.selectDefinitionById(1L)).thenReturn(definition("DRAFT"));
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("START", "START", null), node("PROCESS", "PROCESS", null), node("END", "END", null)));
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(List.of(
                transition("START", "PROCESS", "1"), transition("PROCESS", "END", "1")));

        assertThatThrownBy(() -> service.publishDefinition(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("处理人");
    }

    @Test
    @DisplayName("合法流程发布后应成为当前版本")
    void publishValidDefinitionShouldSucceed() {
        TicketWorkflowDefinition definition = definition("DRAFT");
        definition.setWorkflowKey("LEAVE");
        when(definitionMapper.selectDefinitionById(1L)).thenReturn(definition);
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("START", "START", null), node("PROCESS", "PROCESS", "ROLE"), node("END", "END", null)));
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(List.of(
                transition("START", "PROCESS", "1"), transition("PROCESS", "END", "1")));
        when(definitionMapper.publishDefinition(any())).thenReturn(1);

        service.publishDefinition(1L);

        verify(definitionMapper).clearCurrentByKey("LEAVE");
        verify(definitionMapper).publishDefinition(any(TicketWorkflowDefinition.class));
    }

    @Test
    @DisplayName("分类不能绑定没有发布版本的流程")
    void bindCategoryToMissingWorkflowShouldFail() {
        when(categoryMapper.selectCategoryById(2L)).thenReturn(new TicketCategory());
        when(definitionMapper.selectCurrentDefinitionByKey("MISSING")).thenReturn(null);

        assertThatThrownBy(() -> service.bindCategory(bindDTO()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发布");
        verify(categoryMapper, never()).updateWorkflowKey(any(), any(), any());
    }

    @Test
    @DisplayName("存在环路的流程不允许发布")
    void publishCyclicDefinitionShouldFail() {
        when(definitionMapper.selectDefinitionById(1L)).thenReturn(definition("DRAFT"));
        when(nodeMapper.selectNodeListByDefinitionId(1L)).thenReturn(List.of(
                node("START", "START", null), node("PROCESS", "PROCESS", "ROLE"), node("END", "END", null)));
        TicketWorkflowTransition loop = transition("PROCESS", "START", "0");
        loop.setConditionField("PRIORITY"); loop.setConditionOperator("EQ"); loop.setConditionValue("HIGH");
        when(transitionMapper.selectTransitionListByDefinitionId(1L)).thenReturn(List.of(
                transition("START", "PROCESS", "1"), loop, transition("PROCESS", "END", "1")));

        assertThatThrownBy(() -> service.publishDefinition(1L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("环路");
    }

    private TicketWorkflowDefinitionDTO createValidDefinitionDTO() {
        TicketWorkflowDefinitionDTO dto = new TicketWorkflowDefinitionDTO();
        dto.setWorkflowKey("LEAVE");
        dto.setWorkflowName("请假流程");
        dto.setNodes(List.of(nodeDTO("START", "START", null),
                nodeDTO("PROCESS", "PROCESS", "ROLE"), nodeDTO("END", "END", null)));
        dto.setTransitions(List.of(transitionDTO("START", "PROCESS"), transitionDTO("PROCESS", "END")));
        return dto;
    }

    private TicketWorkflowNodeDTO nodeDTO(String key, String type, String assigneeType) {
        TicketWorkflowNodeDTO dto = new TicketWorkflowNodeDTO();
        dto.setNodeKey(key); dto.setNodeName(key); dto.setNodeType(type); dto.setAssigneeType(assigneeType);
        if ("ROLE".equals(assigneeType)) { dto.setAssigneeValue(1L); }
        return dto;
    }

    private TicketWorkflowTransitionDTO transitionDTO(String source, String target) {
        TicketWorkflowTransitionDTO dto = new TicketWorkflowTransitionDTO();
        dto.setSourceNodeKey(source); dto.setTargetNodeKey(target); dto.setDefaultTransition("1");
        return dto;
    }

    private TicketWorkflowDefinition definition(String status) {
        TicketWorkflowDefinition definition = new TicketWorkflowDefinition();
        definition.setDefinitionId(1L); definition.setWorkflowKey("LEAVE");
        definition.setDefinitionStatus(status); definition.setVersion(1);
        return definition;
    }

    private TicketWorkflowNode node(String key, String type, String assigneeType) {
        TicketWorkflowNode node = new TicketWorkflowNode();
        node.setNodeKey(key); node.setNodeName(key); node.setNodeType(type); node.setAssigneeType(assigneeType);
        if ("ROLE".equals(assigneeType)) { node.setAssigneeValue(1L); }
        return node;
    }

    private TicketWorkflowTransition transition(String source, String target, String defaultTransition) {
        TicketWorkflowTransition transition = new TicketWorkflowTransition();
        transition.setSourceNodeKey(source); transition.setTargetNodeKey(target);
        transition.setDefaultTransition(defaultTransition);
        return transition;
    }

    private TicketWorkflowBindDTO bindDTO() {
        TicketWorkflowBindDTO dto = new TicketWorkflowBindDTO();
        dto.setCategoryId(2L); dto.setWorkflowKey("MISSING");
        return dto;
    }
}
