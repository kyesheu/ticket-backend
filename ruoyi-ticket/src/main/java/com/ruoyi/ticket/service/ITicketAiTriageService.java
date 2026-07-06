package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiTriageDecisionDTO;
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

    /**
     * 采纳或修改后采纳分诊建议。
     *
     * @param suggestionId 建议ID
     * @param dto 最终选择
     */
    void apply(Long suggestionId, TicketAiTriageDecisionDTO dto);

    /**
     * 拒绝分诊建议。
     *
     * @param suggestionId 建议ID
     */
    void reject(Long suggestionId);
}
