package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketWorkflowNode;
import java.util.List;

/**
 * 工单流程Node Mapper 接口
 *
 * @author ticket
 */
public interface TicketWorkflowNodeMapper {

    /**
     * 查询定义节点
     *
     * @param definitionId 定义节点
     * @return 查询结果
     */
    List<TicketWorkflowNode> selectNodeListByDefinitionId(Long definitionId);

    /**
     * 新增流程节点
     *
     * @param node 流程节点
     * @return 影响行数
     */
    int insertNode(TicketWorkflowNode node);

    int deleteNodesByDefinitionId(Long definitionId);
}
