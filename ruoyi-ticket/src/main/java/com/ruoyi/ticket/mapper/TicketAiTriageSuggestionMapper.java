package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketAiTriageSuggestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 分诊建议 Mapper。
 */
@Mapper
public interface TicketAiTriageSuggestionMapper {

    int insertSuggestion(TicketAiTriageSuggestion suggestion);

    TicketAiTriageSuggestion selectById(@Param("suggestionId") Long suggestionId);

    int applyPending(TicketAiTriageSuggestion suggestion);

    int rejectPending(@Param("suggestionId") Long suggestionId,
                      @Param("operatedBy") Long operatedBy);

    int expirePending(@Param("suggestionId") Long suggestionId);

    long countAll();

    long countByStatus(@Param("status") String status);
}
