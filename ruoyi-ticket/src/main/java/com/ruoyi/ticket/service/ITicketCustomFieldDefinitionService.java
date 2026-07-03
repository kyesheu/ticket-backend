package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketCustomFieldDefinitionDTO;
import com.ruoyi.ticket.vo.TicketCustomFieldDefinitionVO;

import java.util.List;

/** 自定义字段定义管理 Service。 */
public interface ITicketCustomFieldDefinitionService {
    List<TicketCustomFieldDefinitionVO> selectByCategoryId(Long categoryId);
    TicketCustomFieldDefinitionVO selectById(Long fieldId);
    Long insertDefinition(TicketCustomFieldDefinitionDTO dto);
    int updateDefinition(Long fieldId, TicketCustomFieldDefinitionDTO dto);
}
