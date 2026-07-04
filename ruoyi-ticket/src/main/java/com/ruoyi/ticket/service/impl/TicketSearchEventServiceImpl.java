package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchEvent;
import com.ruoyi.ticket.enums.TicketSearchEventStatus;
import com.ruoyi.ticket.enums.TicketSearchEventType;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.service.ITicketSearchEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 工单检索事务事件服务实现。
 */
@Service
public class TicketSearchEventServiceImpl implements ITicketSearchEventService {

    @Autowired
    private TicketSearchEventMapper eventMapper;

    @Override
    public void publishUpsert(Long ticketId) {
        Date now = new Date();
        TicketSearchEvent event = new TicketSearchEvent();
        event.setTicketId(ticketId);
        event.setEventType(TicketSearchEventType.UPSERT.name());
        event.setEventStatus(TicketSearchEventStatus.PENDING.name());
        event.setRetryCount(0);
        event.setCreateTime(now);
        event.setUpdateTime(now);
        eventMapper.insertSearchEvent(event);
    }
}
