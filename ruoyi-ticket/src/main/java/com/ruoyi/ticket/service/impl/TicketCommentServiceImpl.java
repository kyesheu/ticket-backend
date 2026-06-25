package com.ruoyi.ticket.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.dto.TicketCommentDTO;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.service.ITicketCommentService;

/**
 * 工单评论 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketCommentServiceImpl implements ITicketCommentService {

    @Autowired
    private TicketCommentMapper ticketCommentMapper;

    @Override
    public List<TicketComment> selectCommentsByTicketId(Long ticketId) {
        // TODO: 阶段六实现
        return null;
    }

    @Override
    public int addComment(Long ticketId, TicketCommentDTO dto) {
        // TODO: 阶段六实现
        return 0;
    }
}
