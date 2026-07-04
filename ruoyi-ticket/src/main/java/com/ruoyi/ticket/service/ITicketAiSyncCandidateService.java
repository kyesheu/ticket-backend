package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import java.util.List;

/**
 * 历史工单同步候选查询 Service 接口。
 */
public interface ITicketAiSyncCandidateService {

    /**
     * 查询并组装可同步的历史工单 DTO。
     *
     * @param lastTicketId 上一批最后工单 ID
     * @param limit 批量大小
     * @return 同步 DTO 列表
     */
    List<TicketAiClosedTicketSyncDTO> selectCandidatesAfter(Long lastTicketId, Integer limit);
}
