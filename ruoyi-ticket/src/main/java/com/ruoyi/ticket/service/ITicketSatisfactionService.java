package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketSatisfactionCreateDTO;
import com.ruoyi.ticket.dto.TicketSatisfactionQueryDTO;
import com.ruoyi.ticket.vo.TicketSatisfactionStatisticsVO;
import com.ruoyi.ticket.vo.TicketSatisfactionVO;

import java.util.List;

/** 工单满意度 Service 接口。 */
public interface ITicketSatisfactionService {
    Long createSatisfaction(Long ticketId, TicketSatisfactionCreateDTO dto);
    TicketSatisfactionVO selectByTicketId(Long ticketId);
    List<TicketSatisfactionVO> selectSatisfactionList(TicketSatisfactionQueryDTO query);
    TicketSatisfactionStatisticsVO selectStatistics(TicketSatisfactionQueryDTO query);
}
