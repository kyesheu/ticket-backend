package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchEvent;
import com.ruoyi.ticket.enums.TicketSearchEventStatus;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.service.ITicketSearchIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 工单检索事件调度服务。
 */
@Service
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchDispatcherServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketSearchDispatcherServiceImpl.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_RETRY_COUNT = 5;
    private static final long BASE_RETRY_DELAY_MILLIS = 30_000L;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final long CLAIM_TIMEOUT_MILLIS = 10L * 60L * 1000L;

    @Autowired private TicketSearchEventMapper eventMapper;
    @Autowired private ITicketSearchIndexer searchIndexer;

    /**
     * 调度一批待处理事件。
     *
     * @return 成功处理数量
     */
    public int dispatchPendingEvents() {
        List<TicketSearchEvent> events = eventMapper.selectDispatchableEvents(
                TicketSearchEventStatus.PENDING.name(), DEFAULT_BATCH_SIZE);
        int processedCount = 0;
        for (TicketSearchEvent event : events) {
            int claimed = eventMapper.claimEvent(event.getEventId(), TicketSearchEventStatus.PENDING.name(),
                    TicketSearchEventStatus.PROCESSING.name());
            if (claimed != 1) {
                continue;
            }
            try {
                searchIndexer.upsertTicket(event.getTicketId(), event.getEventId());
                eventMapper.markSucceeded(event.getEventId(), TicketSearchEventStatus.PROCESSING.name());
                processedCount++;
            } catch (RuntimeException exception) {
                handleFailure(event, exception);
            }
        }
        return processedCount;
    }

    /**
     * 恢复 Worker 异常退出后遗留的超时抢占事件。
     *
     * @return 恢复数量
     */
    public int recoverStaleEvents() {
        Date claimedBefore = new Date(System.currentTimeMillis() - CLAIM_TIMEOUT_MILLIS);
        return eventMapper.recoverStaleEvents(TicketSearchEventStatus.PROCESSING.name(),
                TicketSearchEventStatus.PENDING.name(), claimedBefore);
    }

    private void handleFailure(TicketSearchEvent event, RuntimeException exception) {
        int retryCount = event.getRetryCount() + 1;
        boolean exhausted = retryCount >= MAX_RETRY_COUNT;
        String targetStatus = exhausted
                ? TicketSearchEventStatus.FAILED.name() : TicketSearchEventStatus.PENDING.name();
        Date nextRetryAt = exhausted ? null : calculateNextRetryTime(retryCount);
        String errorMessage = abbreviate(exception.getClass().getSimpleName() + ": 工单索引写入失败");
        eventMapper.markFailed(event.getEventId(), TicketSearchEventStatus.PROCESSING.name(), targetStatus,
                retryCount, nextRetryAt, errorMessage);
        LOGGER.error("工单检索事件处理失败，eventId={}，ticketId={}，retryCount={}，exceptionType={}",
                event.getEventId(), event.getTicketId(), retryCount, exception.getClass().getSimpleName());
    }

    private Date calculateNextRetryTime(int retryCount) {
        long multiplier = 1L << Math.min(retryCount - 1, 10);
        return new Date(System.currentTimeMillis() + BASE_RETRY_DELAY_MILLIS * multiplier);
    }

    private String abbreviate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
