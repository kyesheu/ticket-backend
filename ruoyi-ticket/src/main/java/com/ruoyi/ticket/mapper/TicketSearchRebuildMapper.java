package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketSearchRebuild;
import org.apache.ibatis.annotations.Param;

/** 工单检索全量重建 Mapper。 */
public interface TicketSearchRebuildMapper {
    int recoverStaleRunning(@Param("updatedBefore") java.util.Date updatedBefore);
    int tryStart(TicketSearchRebuild rebuild);
    TicketSearchRebuild selectCurrent();
    int updateProgress(@Param("processedCount") long processedCount, @Param("lastTicketId") Long lastTicketId);
    int markSucceeded(@Param("processedCount") long processedCount);
    int markFailed(@Param("errorMessage") String errorMessage);
}
