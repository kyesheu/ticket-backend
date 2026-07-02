package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketSlaAlert;
import com.ruoyi.ticket.enums.TicketSlaAlertType;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSlaAlertMapper;
import com.ruoyi.ticket.service.ITicketNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单 SLA 超时扫描 Service 测试
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单 SLA 超时扫描 Service 测试")
class TicketSlaServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketSlaAlertMapper ticketSlaAlertMapper;

    @Mock
    private ITicketNotificationService ticketNotificationService;

    @InjectMocks
    private TicketSlaServiceImpl ticketSlaService;

    @Test
    @DisplayName("扫描应分别生成响应超时和解决超时告警")
    void scanShouldCreateBothAlertTypes() {
        Ticket responseOverdue = createTicket(1L, 30);
        Ticket resolutionOverdue = createTicket(2L, 90);
        AtomicInteger responseQueryCount = new AtomicInteger();
        AtomicInteger resolveQueryCount = new AtomicInteger();
        when(ticketMapper.selectResponseOverdueCandidates(any(Date.class), anyInt()))
                .thenAnswer(invocation -> responseQueryCount.getAndIncrement() == 0
                        ? List.of(responseOverdue) : List.of());
        when(ticketMapper.selectResolveOverdueCandidates(any(Date.class), anyInt()))
                .thenAnswer(invocation -> resolveQueryCount.getAndIncrement() == 0
                        ? List.of(resolutionOverdue) : List.of());
        when(ticketMapper.markResponseOverdue(1L)).thenReturn(1);
        when(ticketMapper.markResolveOverdue(2L)).thenReturn(1);

        int alertCount = ticketSlaService.scanOverdue();

        assertThat(alertCount).isEqualTo(2);
        ArgumentCaptor<TicketSlaAlert> captor = ArgumentCaptor.forClass(TicketSlaAlert.class);
        verify(ticketSlaAlertMapper, org.mockito.Mockito.times(2)).insertAlert(captor.capture());
        assertThat(captor.getAllValues()).extracting(TicketSlaAlert::getAlertType)
                .containsExactly(
                        TicketSlaAlertType.RESPONSE_OVERDUE.name(),
                        TicketSlaAlertType.RESOLUTION_OVERDUE.name());
        assertThat(captor.getAllValues()).allMatch(alert -> alert.getOverdueMinutes() >= 0);
    }

    @Test
    @DisplayName("条件更新未抢占到工单时不得重复生成告警")
    void scanShouldSkipAlertWhenConditionalUpdateMisses() {
        Ticket responseOverdue = createTicket(1L, 30);
        when(ticketMapper.selectResponseOverdueCandidates(any(Date.class), anyInt()))
                .thenReturn(List.of(responseOverdue));
        when(ticketMapper.selectResolveOverdueCandidates(any(Date.class), anyInt()))
                .thenReturn(List.of());
        when(ticketMapper.markResponseOverdue(1L)).thenReturn(0);

        int alertCount = ticketSlaService.scanOverdue();

        assertThat(alertCount).isZero();
        verify(ticketSlaAlertMapper, never()).insertAlert(any());
    }

    @Test
    @DisplayName("没有超时工单时扫描结果应为零")
    void scanWithoutCandidatesShouldReturnZero() {
        when(ticketMapper.selectResponseOverdueCandidates(any(Date.class), anyInt()))
                .thenReturn(List.of());
        when(ticketMapper.selectResolveOverdueCandidates(any(Date.class), anyInt()))
                .thenReturn(List.of());

        assertThat(ticketSlaService.scanOverdue()).isZero();
        verify(ticketMapper, never()).markResponseOverdue(anyLong());
        verify(ticketMapper, never()).markResolveOverdue(anyLong());
    }

    private Ticket createTicket(Long ticketId, int overdueMinutes) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setCreatorId(10L);
        ticket.setAssigneeId(20L);
        Date dueAt = new Date(System.currentTimeMillis() - overdueMinutes * 60_000L);
        ticket.setResponseDueAt(dueAt);
        ticket.setResolveDueAt(dueAt);
        return ticket;
    }
}
