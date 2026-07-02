package com.ruoyi.ticket.task;

import com.ruoyi.ticket.service.ITicketSlaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工单 SLA Quartz 调用入口
 *
 * @author ticket
 */
@Component("ticketSlaTask")
public class TicketSlaTask {

    @Autowired
    private ITicketSlaService ticketSlaService;

    /**
     * 扫描超时工单。
     */
    public void scanOverdue() {
        ticketSlaService.scanOverdue();
    }
}
