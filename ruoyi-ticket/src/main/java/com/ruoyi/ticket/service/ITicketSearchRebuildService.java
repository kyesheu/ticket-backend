package com.ruoyi.ticket.service;

import com.ruoyi.ticket.domain.TicketSearchRebuild;

/** 工单检索全量重建服务。 */
public interface ITicketSearchRebuildService {
    void startRebuild();
    TicketSearchRebuild getStatus();
    int retryFailedEvents();
}
