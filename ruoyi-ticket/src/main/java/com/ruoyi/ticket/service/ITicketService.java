package com.ruoyi.ticket.service;

import java.util.List;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.vo.TicketListVO;
import com.ruoyi.ticket.vo.TicketVO;

/**
 * 工单 Service 接口
 *
 * @author ticket
 */
public interface ITicketService {

    /**
     * 分页查询工单列表
     */
    List<TicketListVO> selectTicketList(TicketQueryDTO query);

    /**
     * 查询当前处理人的待办工单。
     */
    List<TicketListVO> selectMyTodoTickets(TicketQueryDTO query);

    /**
     * 查询工单详情（含分类/用户/部门名称、评论列表、操作日志）
     */
    TicketVO selectTicketById(Long ticketId);

    /**
     * 创建工单
     */
    Long createTicket(TicketCreateDTO dto);

    /**
     * 分派工单
     */
    void assignTicket(Long ticketId, TicketAssignDTO dto);

    /**
     * 处理工单
     */
    void processTicket(Long ticketId, TicketProcessDTO dto);

    /**
     * 确认工单
     */
    void confirmTicket(Long ticketId, TicketConfirmDTO dto);

    /**
     * 取消工单
     */
    void cancelTicket(Long ticketId, TicketCancelDTO dto);
}
