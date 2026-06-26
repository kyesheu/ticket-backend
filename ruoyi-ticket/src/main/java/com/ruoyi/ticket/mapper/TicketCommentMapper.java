package com.ruoyi.ticket.mapper;

import java.util.List;
import com.ruoyi.ticket.domain.TicketComment;

/**
 * 工单评论 Mapper 接口
 *
 * @author ticket
 */
public interface TicketCommentMapper {

    /**
     * 根据工单 ID 查询评论列表，按时间倒序
     */
    List<TicketComment> selectCommentsByTicketId(Long ticketId);

    /**
     * 新增评论
     */
    int insertComment(TicketComment comment);
}
