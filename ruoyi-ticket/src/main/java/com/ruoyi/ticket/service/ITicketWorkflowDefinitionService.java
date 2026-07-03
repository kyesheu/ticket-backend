package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketWorkflowBindDTO;
import com.ruoyi.ticket.dto.TicketWorkflowDefinitionDTO;
import com.ruoyi.ticket.vo.TicketWorkflowDefinitionVO;

import java.util.List;

/**
 * 工单流程定义 Service 接口
 *
 * @author ticket
 */
public interface ITicketWorkflowDefinitionService {

    List<TicketWorkflowDefinitionVO> selectDefinitionList();

    TicketWorkflowDefinitionVO selectDefinitionById(Long definitionId);

    Long insertDraft(TicketWorkflowDefinitionDTO dto);

    int updateDraft(Long definitionId, TicketWorkflowDefinitionDTO dto);

    Long copyVersion(Long definitionId);

    int publishDefinition(Long definitionId);

    int bindCategory(TicketWorkflowBindDTO dto);
}
