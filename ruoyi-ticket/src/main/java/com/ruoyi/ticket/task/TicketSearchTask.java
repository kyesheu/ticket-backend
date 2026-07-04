package com.ruoyi.ticket.task;

import com.ruoyi.ticket.service.impl.TicketSearchDispatcherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 工单检索事件 Quartz 调用入口。
 */
@Component("ticketSearchTask")
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchTask {

    @Autowired
    private TicketSearchDispatcherServiceImpl dispatcherService;

    /**
     * 恢复超时事件并调度一批待处理事件。
     */
    public void dispatch() {
        dispatcherService.recoverStaleEvents();
        dispatcherService.dispatchPendingEvents();
    }
}
