package com.ruoyi.ticket.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.service.ITicketOperationLogService;

/**
 * 工单操作日志 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketOperationLogServiceImpl implements ITicketOperationLogService {

    @Autowired
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Override
    public List<TicketOperationLog> selectLogsByTicketId(Long ticketId) {
        // TODO: 阶段六实现
        return null;
    }

    @Override
    public int insertLog(TicketOperationLog log) {
        // TODO: 阶段五/六实现
        return 0;
    }
}
