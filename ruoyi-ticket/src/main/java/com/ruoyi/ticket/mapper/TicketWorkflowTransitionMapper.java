package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketWorkflowTransition;
import java.util.List;

/**
 * 工单流程Transition Mapper 接口
 *
 * @author ticket
 */
public interface TicketWorkflowTransitionMapper {

    /**
     * 查询定义连线
     *
     * @param definitionId 定义连线
     * @return 查询结果
     */
    List<TicketWorkflowTransition> selectTransitionListByDefinitionId(Long definitionId);

    /**
     * 新增流程连线
     *
     * @param transition 流程连线
     * @return 影响行数
     */
    int insertTransition(TicketWorkflowTransition transition);

    int deleteTransitionsByDefinitionId(Long definitionId);
}
