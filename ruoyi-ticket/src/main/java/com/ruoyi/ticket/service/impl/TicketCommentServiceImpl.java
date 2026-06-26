package com.ruoyi.ticket.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.dto.TicketCommentDTO;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketCommentService;

/**
 * 工单评论 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketCommentServiceImpl implements ITicketCommentService {

    /** 默认评论类型 */
    private static final String DEFAULT_COMMENT_TYPE = "EXTERNAL";

    @Autowired
    private TicketCommentMapper ticketCommentMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public List<TicketComment> selectCommentsByTicketId(Long ticketId) {
        return ticketCommentMapper.selectCommentsByTicketId(ticketId);
    }

    @Override
    public int addComment(Long ticketId, TicketCommentDTO dto) {
        // 校验工单存在
        Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        // 评论内容必填
        if (StringUtils.isBlank(dto.getContent())) {
            throw new ServiceException("评论内容不能为空");
        }
        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(SecurityUtils.getUserId());
        comment.setContent(dto.getContent());
        comment.setCommentType(StringUtils.isNotBlank(dto.getCommentType())
                ? dto.getCommentType() : DEFAULT_COMMENT_TYPE);
        comment.setDelFlag("0");
        comment.setCreateBy(SecurityUtils.getUsername());
        comment.setCreateTime(new Date());
        comment.setUpdateBy(SecurityUtils.getUsername());
        comment.setUpdateTime(new Date());
        return ticketCommentMapper.insertComment(comment);
    }
}
