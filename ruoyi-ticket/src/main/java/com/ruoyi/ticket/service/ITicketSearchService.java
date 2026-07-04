package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.vo.TicketSearchResultVO;

/** 工单全文检索服务。 */
public interface ITicketSearchService {
    TicketSearchResultVO search(TicketSearchQueryDTO query);
}
