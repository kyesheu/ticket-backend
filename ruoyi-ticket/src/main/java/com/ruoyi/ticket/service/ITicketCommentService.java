package com.ruoyi.ticket.service;

import java.util.List;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.dto.TicketCommentDTO;

/**
 * 工单评论 Service 接口
 *
 * @author ticket
 */
public interface ITicketCommentService {

    /**
     * 根据工单 ID 查询评论列表
     */
    List<TicketComment> selectCommentsByTicketId(Long ticketId);

    /**
     * 添加评论
     */
    int addComment(Long ticketId, TicketCommentDTO dto);
}
