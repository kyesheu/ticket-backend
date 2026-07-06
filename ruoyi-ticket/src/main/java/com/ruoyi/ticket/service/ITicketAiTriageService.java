package com.ruoyi.ticket.service;

import com.ruoyi.ticket.vo.TicketAiTriageVO;

/**
 * 工单 AI 分诊服务。
 */
public interface ITicketAiTriageService {

    /**
     * 生成工单分诊建议。
     *
     * @param ticketId 工单ID
     * @return 分诊建议
     */
    TicketAiTriageVO triage(Long ticketId);
}
