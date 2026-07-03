package com.ruoyi.ticket.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.dto.TicketCommentDTO;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketCommentService;
import com.ruoyi.ticket.service.ITicketNotificationService;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
import com.ruoyi.ticket.service.ITicketAttachmentService;

/**
 * 工单评论 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketCommentServiceImpl implements ITicketCommentService {

    /** 默认评论类型 */
    private static final String DEFAULT_COMMENT_TYPE = "EXTERNAL";
    private static final String COMMENT_LIST_PERMISSION = "ticket:comment:list";
    private static final String COMMENT_ADD_PERMISSION = "ticket:comment:add";

    @Autowired
    private TicketCommentMapper ticketCommentMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private ITicketNotificationService ticketNotificationService;

    @Autowired
    private ITicketAccessPolicy ticketAccessPolicy;

    @Autowired
    private ITicketAttachmentService ticketAttachmentService;

    @Override
    public List<TicketComment> selectCommentsByTicketId(Long ticketId) {
        ticketAccessPolicy.assertCanAccess(ticketId, COMMENT_LIST_PERMISSION);
        return ticketCommentMapper.selectCommentsByTicketId(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addComment(Long ticketId, TicketCommentDTO dto) {
        ticketAccessPolicy.assertCanAccess(ticketId, COMMENT_ADD_PERMISSION);
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
        int rows = ticketCommentMapper.insertComment(comment);
        ticketAttachmentService.bindAttachments(ticketId, TicketAttachmentBusinessType.COMMENT,
                comment.getCommentId(), dto.getAttachmentIds());
        String eventKey = "COMMENTED:" + comment.getCommentId();
        Long operatorId = SecurityUtils.getUserId();
        ticketNotificationService.createNotification(ticketId, ticket.getCreatorId(), operatorId,
                TicketNotificationType.COMMENTED, eventKey, "工单有新评论", dto.getContent());
        ticketNotificationService.createNotification(ticketId, ticket.getAssigneeId(), operatorId,
                TicketNotificationType.COMMENTED, eventKey, "工单有新评论", dto.getContent());
        return rows;
    }
}
