package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiAskDTO;
import com.ruoyi.ticket.dto.TicketAiEscalateDTO;
import com.ruoyi.ticket.vo.TicketAiEscalateVO;
import com.ruoyi.ticket.vo.TicketAiQuestionAnswerVO;

/**
 * AI 前置问答与转人工编排服务。
 */
public interface ITicketAiQuestionService {

    TicketAiQuestionAnswerVO ask(TicketAiAskDTO dto);

    TicketAiEscalateVO escalate(TicketAiEscalateDTO dto);
}
