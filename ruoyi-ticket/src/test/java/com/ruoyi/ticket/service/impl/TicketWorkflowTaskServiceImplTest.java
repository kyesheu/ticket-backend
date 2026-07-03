package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.mapper.TicketWorkflowTaskMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.vo.TicketWorkflowTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单流程任务查询测试。 */
@ExtendWith(MockitoExtension.class)
class TicketWorkflowTaskServiceImplTest {

    @Mock private TicketWorkflowTaskMapper taskMapper;
    @Mock private ITicketAccessPolicy accessPolicy;
    @InjectMocks private TicketWorkflowTaskServiceImpl service;

    @Test
    void shouldQueryPendingTasksForCurrentUser() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            when(taskMapper.selectPendingTaskList(7L)).thenReturn(List.of(new TicketWorkflowTaskVO()));
            assertThat(service.selectMyPendingTasks()).hasSize(1);
        }
    }

    @Test
    void shouldCheckTicketAccessForTaskDetail() {
        TicketWorkflowTaskVO task = new TicketWorkflowTaskVO(); task.setTicketId(100L);
        when(taskMapper.selectTaskDetail(10L)).thenReturn(task);
        assertThat(service.selectTaskById(10L)).isSameAs(task);
        verify(accessPolicy).assertCanAccess(100L, "ticket:workflow:task");
    }

    @Test
    void shouldRejectMissingTaskDetail() {
        when(taskMapper.selectTaskDetail(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.selectTaskById(99L))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不存在");
    }
}
