package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.service.ITicketAiTriageService;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import java.util.Collections;
import org.springframework.stereotype.Service;

/**
 * 工单 AI 分诊编排服务。
 */
@Service
public class TicketAiTriageServiceImpl implements ITicketAiTriageService {

    @Override
    public TicketAiTriageVO triage(Long ticketId) {
        // TODO: 阶段50实现候选集组装、AI调用和响应校验
        TicketAiTriageVO result = new TicketAiTriageVO();
        result.setSources(Collections.emptyList());
        result.setDegraded(true);
        result.setReason("stage49_contract_only");
        result.setConfidence(0D);
        return result;
    }
}
