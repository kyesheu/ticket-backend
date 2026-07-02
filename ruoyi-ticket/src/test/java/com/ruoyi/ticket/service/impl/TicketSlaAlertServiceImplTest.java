package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketSlaAlertQueryDTO;
import com.ruoyi.ticket.mapper.TicketSlaAlertMapper;
import com.ruoyi.ticket.service.ITicketSlaService;
import com.ruoyi.ticket.vo.TicketSlaAlertVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单 SLA 告警 Service 测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单 SLA 告警 Service 测试")
class TicketSlaAlertServiceImplTest {

    @Mock
    private TicketSlaAlertMapper ticketSlaAlertMapper;

    @Mock
    private ITicketSlaService ticketSlaService;

    @InjectMocks
    private TicketSlaAlertServiceImpl ticketSlaAlertService;

    @Test
    @DisplayName("查询告警列表应透传查询条件")
    void selectAlertListShouldDelegateToMapper() {
        TicketSlaAlertQueryDTO query = new TicketSlaAlertQueryDTO();
        TicketSlaAlertVO alert = new TicketSlaAlertVO();
        when(ticketSlaAlertMapper.selectAlertList(query)).thenReturn(List.of(alert));

        assertThat(ticketSlaAlertService.selectAlertList(query)).containsExactly(alert);
    }

    @Test
    @DisplayName("查询不存在告警应抛异常")
    void selectMissingAlertShouldThrow() {
        when(ticketSlaAlertMapper.selectAlertById(99L)).thenReturn(null);

        assertThatThrownBy(() -> ticketSlaAlertService.selectAlertById(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("告警不存在");
    }

    @Test
    @DisplayName("手工补扫应返回新增告警数量")
    void scanShouldReturnCreatedAlertCount() {
        when(ticketSlaService.scanOverdue()).thenReturn(2);

        assertThat(ticketSlaAlertService.scanOverdue()).isEqualTo(2);
        verify(ticketSlaService).scanOverdue();
    }
}
