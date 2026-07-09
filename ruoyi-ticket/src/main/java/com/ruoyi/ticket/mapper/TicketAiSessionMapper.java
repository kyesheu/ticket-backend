package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketAiEscalation;
import com.ruoyi.ticket.domain.TicketAiSession;
import com.ruoyi.ticket.domain.TicketAiSessionSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 问答会话 Mapper。
 */
@Mapper
public interface TicketAiSessionMapper {

    int insertSession(TicketAiSession session);

    int insertSource(TicketAiSessionSource source);

    int insertEscalation(TicketAiEscalation escalation);

    int markResolved(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    int markEscalated(@Param("sessionId") Long sessionId, @Param("userId") Long userId,
                      @Param("ticketId") Long ticketId);
}
