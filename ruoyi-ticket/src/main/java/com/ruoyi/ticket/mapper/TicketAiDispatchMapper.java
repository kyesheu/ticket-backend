package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketAiDispatchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 自动分派 Mapper。
 */
@Mapper
public interface TicketAiDispatchMapper {

    int countEnabledRule(@Param("categoryId") Long categoryId,
                         @Param("handlerId") Long handlerId,
                         @Param("priority") String priority);

    int insertLog(TicketAiDispatchLog log);
}
