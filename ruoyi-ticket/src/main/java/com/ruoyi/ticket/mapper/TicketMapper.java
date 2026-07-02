package com.ruoyi.ticket.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 查询今天最大的工单编号（用于生成新编号）
     */
    String selectMaxTicketNo(@Param("prefix") String prefix);

    /**
     * 校验用户是否存在
     */
    int checkUserExists(@Param("userId") Long userId);

    /**
     * 查询工单实体（不含 JOIN，用于状态流转时更新）
     */
    Ticket selectTicketEntityById(@Param("ticketId") Long ticketId);

    /** 查询响应超时候选工单。 */
    List<Ticket> selectResponseOverdueCandidates(@Param("detectedAt") java.util.Date detectedAt,
                                                  @Param("limit") int limit);

    /** 查询解决超时候选工单。 */
    List<Ticket> selectResolveOverdueCandidates(@Param("detectedAt") java.util.Date detectedAt,
                                                 @Param("limit") int limit);

    /** 条件标记响应超时，已标记时返回 0。 */
    int markResponseOverdue(@Param("ticketId") Long ticketId);

    /** 条件标记解决超时，已标记时返回 0。 */
    int markResolveOverdue(@Param("ticketId") Long ticketId);
}
