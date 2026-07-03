package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketCustomFieldDefinition;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 工单自定义字段定义 Mapper。 */
public interface TicketCustomFieldDefinitionMapper {
    List<TicketCustomFieldDefinition> selectByCategoryId(Long categoryId);
    List<TicketCustomFieldDefinition> selectEnabledByCategoryId(Long categoryId);
    TicketCustomFieldDefinition selectById(Long fieldId);
    TicketCustomFieldDefinition selectByCategoryAndKey(@Param("categoryId") Long categoryId,
                                                       @Param("fieldKey") String fieldKey);
    int insertDefinition(TicketCustomFieldDefinition definition);
    int updateDefinition(TicketCustomFieldDefinition definition);
}
