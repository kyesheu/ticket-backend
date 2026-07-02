package com.ruoyi.ticket.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.service.ITicketOperationLogService;
import com.ruoyi.ticket.service.ITicketAccessPolicy;

/**
 * 工单操作日志 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketOperationLogServiceImpl implements ITicketOperationLogService {

    private static final String LOG_LIST_PERMISSION = "ticket:log:list";

    @Autowired
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Autowired
    private ITicketAccessPolicy ticketAccessPolicy;

    @Override
    public List<TicketOperationLog> selectLogsByTicketId(Long ticketId) {
        ticketAccessPolicy.assertCanAccess(ticketId, LOG_LIST_PERMISSION);
        return ticketOperationLogMapper.selectLogsByTicketId(ticketId);
    }

    @Override
    public int insertLog(TicketOperationLog log) {
        if (log.getOperateTime() == null) {
            log.setOperateTime(new Date());
        }
        return ticketOperationLogMapper.insertLog(log);
    }
}
