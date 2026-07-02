package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketSatisfaction;

import java.util.List;

/** 工单满意度 Mapper。 */
public interface TicketSatisfactionMapper {
    TicketSatisfaction selectByTicketId(Long ticketId);
    List<TicketSatisfaction> selectSatisfactionList();
    int insertSatisfaction(TicketSatisfaction satisfaction);
}
