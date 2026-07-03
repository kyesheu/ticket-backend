package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketWorkflowTask;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.ticket.vo.TicketWorkflowTaskVO;
import java.util.List;

/**
 * 工单流程Task Mapper 接口
 *
 * @author ticket
 */
public interface TicketWorkflowTaskMapper {

    /**
     * 查询实例当前待办
     *
     * @param instanceId 实例当前待办
     * @return 查询结果
     */
    TicketWorkflowTask selectPendingTaskByInstanceId(Long instanceId);

    /**
     * 新增流程任务
     *
     * @param task 流程任务
     * @return 影响行数
     */
    int insertTask(TicketWorkflowTask task);

    TicketWorkflowTask selectTaskById(Long taskId);

    TicketWorkflowTask selectPreviousCompletedTask(@Param("instanceId") Long instanceId,
                                                    @Param("taskId") Long taskId);

    int completePendingTask(TicketWorkflowTask task);

    int closePendingTasks(@Param("instanceId") Long instanceId,
                          @Param("taskStatus") String taskStatus,
                          @Param("completedBy") Long completedBy,
                          @Param("completedAt") java.util.Date completedAt);

    List<TicketWorkflowTaskVO> selectPendingTaskList(Long userId);

    TicketWorkflowTaskVO selectTaskDetail(Long taskId);
}
