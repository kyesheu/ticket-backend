package com.ruoyi.ticket.service;

import java.util.List;
import com.ruoyi.ticket.domain.TicketOperationLog;

/**
 * 工单操作日志 Service 接口
 *
 * @author ticket
 */
public interface ITicketOperationLogService {

    /**
     * 根据工单 ID 查询操作日志列表
     */
    List<TicketOperationLog> selectLogsByTicketId(Long ticketId);

    /**
     * 写入操作日志
     */
    int insertLog(TicketOperationLog log);
}
