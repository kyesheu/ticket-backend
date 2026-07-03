package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketCustomFieldOption;

import java.util.List;

/** 工单自定义字段选项 Mapper。 */
public interface TicketCustomFieldOptionMapper {
    List<TicketCustomFieldOption> selectByFieldId(Long fieldId);
    List<TicketCustomFieldOption> selectEnabledByFieldId(Long fieldId);
    TicketCustomFieldOption selectById(Long optionId);
    int insertOption(TicketCustomFieldOption option);
    int updateOption(TicketCustomFieldOption option);
    int disableByFieldId(@org.apache.ibatis.annotations.Param("fieldId") Long fieldId,
                         @org.apache.ibatis.annotations.Param("updateBy") String updateBy);
}
