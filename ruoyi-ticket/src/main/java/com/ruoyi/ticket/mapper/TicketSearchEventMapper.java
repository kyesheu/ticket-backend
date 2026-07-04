package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketSearchEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单检索事件 Mapper。
 */
public interface TicketSearchEventMapper {

    /**
     * 新增工单检索事件。
     *
     * @param event 检索事件
     * @return 影响行数
     */
    int insertSearchEvent(TicketSearchEvent event);

    /**
     * 按事件 ID 顺序查询当前可调度事件。
     *
     * @param eventStatus 事件状态
     * @param limit 最大返回数量
     * @return 可调度事件
     */
    List<TicketSearchEvent> selectDispatchableEvents(@Param("eventStatus") String eventStatus,
                                                     @Param("limit") int limit);

    int claimEvent(@Param("eventId") Long eventId,
                   @Param("expectedStatus") String expectedStatus,
                   @Param("targetStatus") String targetStatus);

    int markSucceeded(@Param("eventId") Long eventId,
                      @Param("expectedStatus") String expectedStatus);

    int markFailed(@Param("eventId") Long eventId,
                   @Param("expectedStatus") String expectedStatus,
                   @Param("targetStatus") String targetStatus,
                   @Param("retryCount") int retryCount,
                   @Param("nextRetryAt") java.util.Date nextRetryAt,
                   @Param("errorMessage") String errorMessage);

    int recoverStaleEvents(@Param("processingStatus") String processingStatus,
                           @Param("pendingStatus") String pendingStatus,
                           @Param("claimedBefore") java.util.Date claimedBefore);

    Long selectMaxEventId();

    int retryFailedEvents();

    int requeueEventsAfter(@Param("eventId") Long eventId);
}
