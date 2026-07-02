package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketSatisfaction;
import com.ruoyi.ticket.dto.TicketSatisfactionCreateDTO;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSatisfactionMapper;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单满意度 Service 测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单满意度 Service 测试")
class TicketSatisfactionServiceImplTest {

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketSatisfactionMapper ticketSatisfactionMapper;
    @Mock private ITicketAccessPolicy ticketAccessPolicy;
    @InjectMocks private TicketSatisfactionServiceImpl ticketSatisfactionService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(7L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("创建人可评价已关闭工单")
    void creatorShouldRateClosedTicket() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(closedTicket(7L));
        when(ticketSatisfactionMapper.selectByTicketId(1L)).thenReturn(null);
        when(ticketSatisfactionMapper.insertSatisfaction(any())).thenAnswer(invocation -> {
            TicketSatisfaction satisfaction = invocation.getArgument(0);
            satisfaction.setSatisfactionId(10L);
            return 1;
        });

        assertThat(ticketSatisfactionService.createSatisfaction(1L, dto(5))).isEqualTo(10L);
        verify(ticketAccessPolicy).assertCanAccess(1L, "ticket:satisfaction:add");
    }

    @Test
    @DisplayName("查询评价详情前应校验工单访问范围")
    void querySatisfactionShouldCheckAccess() {
        ticketSatisfactionService.selectByTicketId(2L);

        verify(ticketAccessPolicy).assertCanAccess(2L, "ticket:satisfaction:query");
        verify(ticketSatisfactionMapper).selectVOByTicketId(2L);
    }

    @Test
    @DisplayName("非创建人不能评价")
    void nonCreatorShouldNotRate() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(closedTicket(8L));

        assertThatThrownBy(() -> ticketSatisfactionService.createSatisfaction(1L, dto(5)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("创建人");
        verify(ticketSatisfactionMapper, never()).insertSatisfaction(any());
    }

    @Test
    @DisplayName("非关闭工单不能评价")
    void openTicketShouldNotBeRated() {
        Ticket ticket = closedTicket(7L);
        ticket.setStatus(TicketStatus.WAIT_CONFIRM.name());
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(ticket);

        assertThatThrownBy(() -> ticketSatisfactionService.createSatisfaction(1L, dto(5)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已关闭");
    }

    @Test
    @DisplayName("重复评价应拒绝")
    void duplicateRatingShouldThrow() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(closedTicket(7L));
        when(ticketSatisfactionMapper.selectByTicketId(1L)).thenReturn(new TicketSatisfaction());

        assertThatThrownBy(() -> ticketSatisfactionService.createSatisfaction(1L, dto(5)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已评价");
    }

    @Test
    @DisplayName("越界评分应拒绝")
    void invalidScoreShouldThrow() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(closedTicket(7L));

        assertThatThrownBy(() -> ticketSatisfactionService.createSatisfaction(1L, dto(6)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("1 到 5");
    }

    private Ticket closedTicket(Long creatorId) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setCreatorId(creatorId);
        ticket.setStatus(TicketStatus.CLOSED.name());
        return ticket;
    }

    private TicketSatisfactionCreateDTO dto(int score) {
        TicketSatisfactionCreateDTO dto = new TicketSatisfactionCreateDTO();
        dto.setScore(score);
        dto.setContent("满意");
        return dto;
    }
}
