package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketSearchIndexer;
import com.ruoyi.ticket.service.TicketSearchDocumentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工单检索索引器实现。
 */
@Service
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchIndexerImpl implements ITicketSearchIndexer {

    private static final String COMMENT_SEPARATOR = "\n";

    @Autowired private TicketMapper ticketMapper;
    @Autowired private TicketCommentMapper commentMapper;
    @Autowired private TicketSearchDocumentGateway documentGateway;

    @Override
    public void upsertTicket(Long ticketId, Long sourceEventId) {
        indexTicketTo(ticketId, sourceEventId, null);
    }

    @Override
    public void indexTicketTo(Long ticketId, Long sourceEventId, String indexName) {
        Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
        if (ticket == null) {
            throw new ServiceException("待索引工单不存在");
        }
        List<TicketComment> comments = commentMapper.selectCommentsByTicketId(ticketId);
        TicketSearchDocument document = toDocument(ticket, comments, sourceEventId);
        if (indexName == null) {
            documentGateway.upsert(document);
        } else {
            documentGateway.upsertToIndex(document, indexName);
        }
    }

    private TicketSearchDocument toDocument(Ticket ticket, List<TicketComment> comments, Long sourceEventId) {
        TicketSearchDocument document = new TicketSearchDocument();
        document.setTicketId(ticket.getTicketId());
        document.setSourceEventId(sourceEventId);
        document.setTicketNo(ticket.getTicketNo());
        document.setTitle(ticket.getTitle());
        document.setContent(ticket.getContent());
        document.setComments(comments.stream().map(TicketComment::getContent)
                .collect(Collectors.joining(COMMENT_SEPARATOR)));
        document.setCategoryId(ticket.getCategoryId());
        document.setPriority(ticket.getPriority());
        document.setStatus(ticket.getStatus());
        document.setCreatorId(ticket.getCreatorId());
        document.setAssigneeId(ticket.getAssigneeId());
        document.setDeptId(ticket.getDeptId());
        document.setCreateTime(ticket.getCreateTime());
        document.setUpdateTime(ticket.getUpdateTime());
        return document;
    }
}
