package com.ruoyi.ticket.service;

import java.util.List;

import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import com.ruoyi.ticket.dto.TicketCustomFieldInputDTO;
import com.ruoyi.ticket.vo.TicketCustomFieldDefinitionVO;

/**
 * 工单自定义字段值服务。
 */
public interface ITicketCustomFieldService {

    /** 查询分类下可填写的字段定义。 */
    List<TicketCustomFieldDefinitionVO> selectFormDefinitions(Long categoryId);

    /** 校验输入并保存不可变值快照。 */
    void validateAndSave(Long ticketId, Long categoryId, List<TicketCustomFieldInputDTO> inputs);

    /** 查询工单字段值快照。 */
    List<TicketCustomFieldValue> selectValueSnapshots(Long ticketId);
}
