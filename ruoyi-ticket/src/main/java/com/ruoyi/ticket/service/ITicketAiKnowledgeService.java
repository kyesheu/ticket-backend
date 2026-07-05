package com.ruoyi.ticket.service;

import com.ruoyi.ticket.vo.TicketAiSearchResultVO;

/**
 * 历史工单同步与相似知识检索业务入口。
 */
public interface ITicketAiKnowledgeService {

    /** 同步一批符合条件的历史工单快照。 */
    int syncClosedTickets(Long lastTicketId, Integer limit);

    /** 在对象权限校验后检索当前工单的相似知识。 */
    TicketAiSearchResultVO searchSimilarKnowledge(Long ticketId);
}
