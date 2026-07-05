package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.dto.TicketAiContextDTO;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAiKnowledgeService;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.ITicketAiSyncCandidateService;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;
import com.ruoyi.ticket.vo.TicketVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 历史工单同步与相似知识检索 Service 实现。
 */
@Service
@ConditionalOnProperty(prefix = "ticket.ai", name = "enabled", havingValue = "true")
public class TicketAiKnowledgeServiceImpl implements ITicketAiKnowledgeService {

    private static final String TICKET_QUERY_PERMISSION = "ticket:ticket:query";

    private final ITicketAiSyncCandidateService candidateService;
    private final ITicketAiService ticketAiService;
    private final ITicketAccessPolicy accessPolicy;
    private final TicketMapper ticketMapper;

    public TicketAiKnowledgeServiceImpl(ITicketAiSyncCandidateService candidateService,
                                        ITicketAiService ticketAiService,
                                        ITicketAccessPolicy accessPolicy,
                                        TicketMapper ticketMapper) {
        this.candidateService = candidateService;
        this.ticketAiService = ticketAiService;
        this.accessPolicy = accessPolicy;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public int syncClosedTickets(Long lastTicketId, Integer limit) {
        int count = 0;
        for (TicketAiClosedTicketSyncDTO dto : candidateService.selectCandidatesAfter(lastTicketId, limit)) {
            ticketAiService.syncClosedTicket(dto);
            count++;
        }
        return count;
    }

    @Override
    public TicketAiSearchResultVO searchSimilarKnowledge(Long ticketId) {
        accessPolicy.assertCanAccess(ticketId, TICKET_QUERY_PERMISSION);
        TicketVO ticket = ticketMapper.selectTicketById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        TicketAiContextDTO context = new TicketAiContextDTO();
        context.setTicketNo(ticket.getTicketNo());
        context.setTitle(ticket.getTitle());
        context.setDescription(ticket.getContent());
        context.setCategoryName(ticket.getCategoryName());
        context.setPriority(ticket.getPriority());
        return ticketAiService.search(context);
    }
}
