package com.ruoyi.ticket.mapper;

import java.util.List;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.vo.TicketListVO;
import com.ruoyi.ticket.vo.TicketVO;

/**
 * 工单 Mapper 接口
 *
 * @author ticket
 */
public interface TicketMapper {

    /**
     * 分页查询工单列表
     */
    List<TicketListVO> selectTicketList(TicketQueryDTO query);

    /**
     * 根据 ID 查询工单详情（含 JOIN 分类/用户/部门名称）
     */
    TicketVO selectTicketById(Long ticketId);

    /**
     * 新增工单
     */
    int insertTicket(Ticket ticket);

    /**
     * 更新工单
     */
    int updateTicket(Ticket ticket);

    /**
     * 删除工单（逻辑删除，设置 del_flag）
     */
    int deleteTicketById(Long ticketId);
}
