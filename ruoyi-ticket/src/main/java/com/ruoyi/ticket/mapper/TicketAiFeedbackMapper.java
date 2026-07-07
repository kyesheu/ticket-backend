package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketAiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 建议反馈 Mapper。
 */
@Mapper
public interface TicketAiFeedbackMapper {

    int insertFeedback(TicketAiFeedback feedback);

    TicketAiFeedback selectByEvaluatorAndTarget(@Param("evaluatorId") Long evaluatorId,
                                                @Param("targetType") String targetType,
                                                @Param("targetId") Long targetId);
}
