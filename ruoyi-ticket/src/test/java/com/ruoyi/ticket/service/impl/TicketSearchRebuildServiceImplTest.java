package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.mapper.TicketSearchRebuildMapper;
import com.ruoyi.ticket.mapper.TicketSearchEventMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单检索全量重建服务测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单检索全量重建服务测试")
class TicketSearchRebuildServiceImplTest {
    @Mock private TicketSearchRebuildMapper rebuildMapper;
    @Mock private TicketSearchEventMapper eventMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketSearchRebuildWorker rebuildWorker;
    @Mock private TaskExecutor ticketSearchTaskExecutor;
    @InjectMocks private TicketSearchRebuildServiceImpl rebuildService;

    @Test
    @DisplayName("原子抢占成功后异步执行重建")
    void shouldStartRebuildOnlyAfterAtomicClaim() {
        when(rebuildMapper.tryStart(any())).thenReturn(1);

        rebuildService.startRebuild();

        verify(rebuildMapper).recoverStaleRunning(any(java.util.Date.class));
        verify(ticketSearchTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("已有运行任务时拒绝并发重建")
    void shouldRejectConcurrentRebuild() {
        when(rebuildMapper.tryStart(any())).thenReturn(0);

        assertThatThrownBy(() -> rebuildService.startRebuild())
                .isInstanceOf(ServiceException.class).hasMessageContaining("正在执行");
        verify(ticketSearchTaskExecutor, never()).execute(any(Runnable.class));
    }
}
