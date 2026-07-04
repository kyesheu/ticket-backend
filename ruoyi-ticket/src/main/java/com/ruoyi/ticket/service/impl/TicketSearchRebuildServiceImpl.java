package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.domain.TicketSearchRebuild;
import com.ruoyi.ticket.enums.TicketSearchRebuildStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.mapper.TicketSearchRebuildMapper;
import com.ruoyi.ticket.service.ITicketSearchRebuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/** 工单检索全量重建服务实现。 */
@Service
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchRebuildServiceImpl implements ITicketSearchRebuildService {
    private static final DateTimeFormatter INDEX_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired private TicketSearchRebuildMapper rebuildMapper;
    @Autowired private TicketSearchEventMapper eventMapper;
    @Autowired private TicketMapper ticketMapper;
    @Autowired private TicketSearchRebuildWorker rebuildWorker;
    @Autowired @Qualifier("ticketSearchTaskExecutor") private TaskExecutor ticketSearchTaskExecutor;

    @Override
    public void startRebuild() {
        Date now = new Date();
        TicketSearchRebuild rebuild = new TicketSearchRebuild();
        rebuild.setRebuildId(1L);
        rebuild.setRebuildStatus(TicketSearchRebuildStatus.RUNNING.name());
        rebuild.setIndexName("ticket-search-v" + LocalDateTime.now().format(INDEX_TIME_FORMAT));
        Long maxTicketId = ticketMapper.selectMaxSearchableTicketId();
        rebuild.setMaxTicketId(maxTicketId == null ? 0L : maxTicketId);
        rebuild.setTotalCount(ticketMapper.countSearchableTicketsUpTo(rebuild.getMaxTicketId()));
        Long maxEventId = eventMapper.selectMaxEventId();
        rebuild.setStartEventId(maxEventId == null ? 0L : maxEventId);
        rebuild.setStartedAt(now);
        rebuild.setUpdateTime(now);
        if (rebuildMapper.tryStart(rebuild) != 1) {
            throw new ServiceException("工单索引重建正在执行");
        }
        ticketSearchTaskExecutor.execute(() -> rebuildWorker.rebuild(rebuild));
    }

    @Override
    public TicketSearchRebuild getStatus() {
        return rebuildMapper.selectCurrent();
    }

    @Override
    public int retryFailedEvents() {
        return eventMapper.retryFailedEvents();
    }
}
