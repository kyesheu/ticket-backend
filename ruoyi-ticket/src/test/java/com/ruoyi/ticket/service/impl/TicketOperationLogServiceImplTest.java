package com.ruoyi.ticket.service.impl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;

/**
 * 工单操作日志 Service 单元测试。
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单操作日志 Service 测试")
class TicketOperationLogServiceImplTest {

    @Mock
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Mock
    private ITicketAccessPolicy ticketAccessPolicy;

    @InjectMocks
    private TicketOperationLogServiceImpl operationLogService;

    @Test
    @DisplayName("查询日志前应校验工单访问范围")
    void selectLogsShouldCheckAccess() {
        operationLogService.selectLogsByTicketId(3L);

        verify(ticketAccessPolicy).assertCanAccess(3L, "ticket:log:list");
        verify(ticketOperationLogMapper).selectLogsByTicketId(3L);
    }
}
