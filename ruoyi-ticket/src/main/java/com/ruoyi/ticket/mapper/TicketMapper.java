package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.model.TicketAiSyncCandidate;
import com.ruoyi.ticket.vo.TicketListVO;
import com.ruoyi.ticket.vo.TicketVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单 Mapper 接口
 *
 * @author ticket
 */
@Mapper
public interface TicketMapper {

    /**
     * 按工单主键游标查询可同步的已关闭工单。
     *
     * @param lastTicketId 上一批最后工单 ID
     * @param limit 批量大小
     * @return 带非空处理结果的已关闭工单投影
     */
    List<TicketAiSyncCandidate> selectAiSyncCandidatesAfter(@Param("lastTicketId") Long lastTicketId,
                                                            @Param("limit") Integer limit);

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

    /**
     * 统计当前范围内的指定工单，用于对象级访问校验。
     */
    int countAccessibleTicket(@Param("ticketId") Long ticketId,
                              @Param("scope") TicketAccessScope scope);

    List<Long> selectAccessibleTicketIds(@Param("ticketIds") List<Long> ticketIds,
                                         @Param("scope") TicketAccessScope scope);

    Long selectMaxSearchableTicketId();

    long countSearchableTicketsUpTo(@Param("maxTicketId") Long maxTicketId);

    List<Long> selectSearchableTicketIdsAfter(@Param("lastTicketId") Long lastTicketId,
                                              @Param("maxTicketId") Long maxTicketId,
                                              @Param("limit") int limit);

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

    int updateAiTriageFields(@Param("ticketId") Long ticketId,
                             @Param("categoryId") Long categoryId,
                             @Param("priority") String priority,
                             @Param("updateBy") String updateBy);

    int updateAiDispatchResult(@Param("ticketId") Long ticketId,
                               @Param("dispatchMode") String dispatchMode,
                               @Param("dispatchReason") String dispatchReason,
                               @Param("updateBy") String updateBy);
}
