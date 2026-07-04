package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.model.TicketSearchCandidatePage;

import java.util.List;

/** 工单 Elasticsearch 查询网关。 */
public interface TicketSearchQueryGateway {
    TicketSearchCandidatePage search(TicketSearchQueryDTO query, TicketAccessScope scope,
                                     List<String> searchAfter, int size);
}
