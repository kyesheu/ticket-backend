package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 工单自定义字段值 Mapper。 */
public interface TicketCustomFieldValueMapper {
    List<TicketCustomFieldValue> selectByTicketId(Long ticketId);
    TicketCustomFieldValue selectByTicketAndKey(@Param("ticketId") Long ticketId,
                                                @Param("fieldKey") String fieldKey);
    int insertValue(TicketCustomFieldValue value);
}
