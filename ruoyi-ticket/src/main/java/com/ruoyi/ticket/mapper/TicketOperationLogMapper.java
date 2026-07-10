package com.ruoyi.ticket.mapper;

import java.util.List;
import com.ruoyi.ticket.domain.TicketOperationLog;

/**
 * 工单操作日志 Mapper 接口
 *
 * @author ticket
 */
public interface TicketOperationLogMapper {

    /**
     * 根据工单 ID 查询操作日志，按时间倒序
     */
    List<TicketOperationLog> selectLogsByTicketId(Long ticketId);

    TicketOperationLog selectLatestProcessLog(Long ticketId);

    /**
     * 新增操作日志
     */
    int insertLog(TicketOperationLog log);
}
