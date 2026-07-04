package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.TicketSearchRebuild;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.mapper.TicketSearchRebuildMapper;
import com.ruoyi.ticket.service.ITicketSearchIndexer;
import com.ruoyi.ticket.service.TicketSearchIndexAdminGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** 工单检索全量重建 Worker。 */
@Component
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchRebuildWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketSearchRebuildWorker.class);
    private static final int BATCH_SIZE = 200;

    @Autowired private TicketMapper ticketMapper;
    @Autowired private TicketSearchRebuildMapper rebuildMapper;
    @Autowired private TicketSearchEventMapper eventMapper;
    @Autowired private ITicketSearchIndexer searchIndexer;
    @Autowired private TicketSearchIndexAdminGateway indexAdminGateway;

    /** 执行一次全量重建。 */
    public void rebuild(TicketSearchRebuild rebuild) {
        long processedCount = 0;
        Long lastTicketId = 0L;
        try {
            indexAdminGateway.createIndex(rebuild.getIndexName());
            while (true) {
                List<Long> ticketIds = ticketMapper.selectSearchableTicketIdsAfter(
                        lastTicketId, rebuild.getMaxTicketId(), BATCH_SIZE);
                if (ticketIds.isEmpty()) { break; }
                for (Long ticketId : ticketIds) {
                    searchIndexer.indexTicketTo(ticketId, rebuild.getStartEventId(), rebuild.getIndexName());
                    processedCount++;
                    lastTicketId = ticketId;
                }
                rebuildMapper.updateProgress(processedCount, lastTicketId);
            }
            indexAdminGateway.refreshIndex(rebuild.getIndexName());
            long indexedCount = indexAdminGateway.countDocuments(rebuild.getIndexName());
            if (indexedCount != rebuild.getTotalCount()) {
                throw new IllegalStateException("索引文档数量校验失败");
            }
            indexAdminGateway.switchAlias(rebuild.getIndexName());
            eventMapper.requeueEventsAfter(rebuild.getStartEventId());
            rebuildMapper.markSucceeded(processedCount);
        } catch (RuntimeException exception) {
            String errorMessage = exception.getClass().getSimpleName() + ": 工单索引重建失败";
            rebuildMapper.markFailed(errorMessage);
            LOGGER.error("工单检索全量重建失败，indexName={}，exceptionType={}",
                    rebuild.getIndexName(), exception.getClass().getSimpleName());
        }
    }
}
