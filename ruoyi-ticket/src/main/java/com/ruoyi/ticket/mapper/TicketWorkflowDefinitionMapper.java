package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketWorkflowDefinition;
import java.util.List;

/**
 * 工单流程Definition Mapper 接口
 *
 * @author ticket
 */
public interface TicketWorkflowDefinitionMapper {

    /**
     * 根据 ID 查询流程定义
     *
     * @param definitionId 流程定义
     * @return 查询结果
     */
    TicketWorkflowDefinition selectDefinitionById(Long definitionId);

    /**
     * 查询当前发布定义
     *
     * @param workflowKey 当前发布定义
     * @return 查询结果
     */
    TicketWorkflowDefinition selectCurrentDefinitionByKey(String workflowKey);

    /**
     * 查询流程定义列表
     *
     * @return 查询结果
     */
    List<TicketWorkflowDefinition> selectDefinitionList();

    /**
     * 新增流程定义
     *
     * @param definition 流程定义
     * @return 影响行数
     */
    int insertDefinition(TicketWorkflowDefinition definition);

    TicketWorkflowDefinition selectLatestDefinitionByKey(String workflowKey);

    TicketWorkflowDefinition selectLatestDefinitionByKeyForUpdate(String workflowKey);

    int updateDefinition(TicketWorkflowDefinition definition);

    int clearCurrentByKey(String workflowKey);

    int publishDefinition(TicketWorkflowDefinition definition);
}
