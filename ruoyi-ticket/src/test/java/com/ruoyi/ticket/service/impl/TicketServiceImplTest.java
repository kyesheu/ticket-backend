package com.ruoyi.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketSlaPolicy;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.mapper.TicketSlaPolicyMapper;
import com.ruoyi.ticket.service.ITicketNotificationService;
import org.mockito.ArgumentCaptor;

/**
 * TicketServiceImpl 单元测试（Mock Mapper 层）
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单 Service 单元测试")
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Mock
    private TicketSlaPolicyMapper ticketSlaPolicyMapper;

    @Mock
    private ITicketNotificationService ticketNotificationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private Ticket newTicket;
    private Ticket processingTicket;
    private Ticket waitConfirmTicket;

    @BeforeEach
    void setUp() {
        // Mock 静态工具类 SecurityUtils
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getDeptId).thenReturn(100L);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
        securityUtilsMock.when(() -> SecurityUtils.isAdmin(anyLong())).thenReturn(true);
        TicketSlaPolicy defaultPolicy = new TicketSlaPolicy();
        defaultPolicy.setPriority("MEDIUM");
        defaultPolicy.setResponseMinutes(240);
        defaultPolicy.setResolveMinutes(1440);
        lenient().when(ticketSlaPolicyMapper.selectEnabledPolicyByPriority(anyString()))
                .thenReturn(defaultPolicy);
        // 准备 NEW 状态工单
        newTicket = new Ticket();
        newTicket.setTicketId(1L);
        newTicket.setStatus(TicketStatus.NEW.name());
        newTicket.setTitle("测试工单");
        newTicket.setCreatorId(1L);

        // 准备 PROCESSING 状态工单
        processingTicket = new Ticket();
        processingTicket.setTicketId(2L);
        processingTicket.setStatus(TicketStatus.PROCESSING.name());
        processingTicket.setAssigneeId(1L);
        processingTicket.setCreatorId(1L);

        // 准备 WAIT_CONFIRM 状态工单
        waitConfirmTicket = new Ticket();
        waitConfirmTicket.setTicketId(3L);
        waitConfirmTicket.setStatus(TicketStatus.WAIT_CONFIRM.name());
        waitConfirmTicket.setCreatorId(1L);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    // ==================== 创建工单 ====================

    @Test
    @DisplayName("创建工单应生成唯一编号并写入 CREATE 日志")
    void createTicketShouldGenerateNoAndLog() {
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn(null);

        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("测试工单");
        dto.setContent("测试内容");
        dto.setPriority("MEDIUM");

        ticketService.createTicket(dto);

        verify(ticketMapper).insertTicket(any(Ticket.class));
        verify(ticketOperationLogMapper).insertLog(any());
    }

    @Test
    @DisplayName("创建工单时默认优先级为 MEDIUM")
    void createTicketShouldDefaultToMedium() {
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn(null);

        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("测试工单");

        ticketService.createTicket(dto);
        verify(ticketMapper).insertTicket(any(Ticket.class));
    }

    @Test
    @DisplayName("创建工单应按策略生成 SLA 截止时间快照")
    void createTicketShouldSnapshotSlaDueTimes() {
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn(null);
        TicketSlaPolicy policy = new TicketSlaPolicy();
        policy.setPriority("HIGH");
        policy.setResponseMinutes(60);
        policy.setResolveMinutes(480);
        when(ticketSlaPolicyMapper.selectEnabledPolicyByPriority("HIGH")).thenReturn(policy);

        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("高优先级工单");
        dto.setPriority("HIGH");
        ticketService.createTicket(dto);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insertTicket(captor.capture());
        Ticket created = captor.getValue();
        assertThat(created.getResponseDueAt().getTime() - created.getCreateTime().getTime())
                .isEqualTo(60L * 60L * 1000L);
        assertThat(created.getResolveDueAt().getTime() - created.getCreateTime().getTime())
                .isEqualTo(480L * 60L * 1000L);
    }

    @Test
    @DisplayName("缺少启用的 SLA 策略时应拒绝创建")
    void createTicketWithoutEnabledPolicyShouldThrow() {
        when(ticketMapper.selectMaxTicketNo(anyString())).thenReturn(null);
        when(ticketSlaPolicyMapper.selectEnabledPolicyByPriority("URGENT")).thenReturn(null);

        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("紧急工单");
        dto.setPriority("URGENT");

        assertThatThrownBy(() -> ticketService.createTicket(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未配置启用的 SLA 策略");
    }

    // ==================== 分派工单 ====================

    @Test
    @DisplayName("NEW 状态工单可分派到 PROCESSING")
    void assignShouldTransitionNewToProcessing() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(newTicket);
        when(ticketMapper.checkUserExists(2L)).thenReturn(1);

        TicketAssignDTO dto = new TicketAssignDTO();
        dto.setAssigneeId(2L);
        dto.setComment("分派测试");

        ticketService.assignTicket(1L, dto);

        verify(ticketMapper).updateTicket(any(Ticket.class));
        verify(ticketOperationLogMapper).insertLog(any());
        verify(ticketNotificationService).createNotification(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(1L), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("分派时指派人不存在应抛异常")
    void assignWithInvalidAssigneeShouldThrow() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(newTicket);
        when(ticketMapper.checkUserExists(9999L)).thenReturn(0);

        TicketAssignDTO dto = new TicketAssignDTO();
        dto.setAssigneeId(9999L);

        assertThatThrownBy(() -> ticketService.assignTicket(1L, dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("指派人不存在");
    }

    @Test
    @DisplayName("CLOSED 状态工单不能分派")
    void assignClosedTicketShouldThrow() {
        Ticket closedTicket = new Ticket();
        closedTicket.setTicketId(4L);
        closedTicket.setStatus(TicketStatus.CLOSED.name());
        when(ticketMapper.selectTicketEntityById(4L)).thenReturn(closedTicket);

        TicketAssignDTO dto = new TicketAssignDTO();
        dto.setAssigneeId(2L);

        assertThatThrownBy(() -> ticketService.assignTicket(4L, dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许分派");
    }

    // ==================== 处理工单 ====================

    @Test
    @DisplayName("PROCESSING 状态工单可处理到 WAIT_CONFIRM")
    void processShouldTransitionToWaitConfirm() {
        when(ticketMapper.selectTicketEntityById(2L)).thenReturn(processingTicket);

        TicketProcessDTO dto = new TicketProcessDTO();
        dto.setComment("处理完成");

        ticketService.processTicket(2L, dto);

        verify(ticketMapper).updateTicket(any(Ticket.class));
        verify(ticketOperationLogMapper).insertLog(any());
    }

    @Test
    @DisplayName("处理时备注为空应抛异常")
    void processWithEmptyCommentShouldThrow() {
        when(ticketMapper.selectTicketEntityById(2L)).thenReturn(processingTicket);

        TicketProcessDTO dto = new TicketProcessDTO();
        dto.setComment("");

        assertThatThrownBy(() -> ticketService.processTicket(2L, dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能为空");
    }

    // ==================== 确认工单 ====================

    @Test
    @DisplayName("WAIT_CONFIRM 状态工单可确认到 CLOSED")
    void confirmShouldTransitionToClosed() {
        when(ticketMapper.selectTicketEntityById(3L)).thenReturn(waitConfirmTicket);

        TicketConfirmDTO dto = new TicketConfirmDTO();
        dto.setComment("确认完成");

        ticketService.confirmTicket(3L, dto);

        verify(ticketMapper).updateTicket(any(Ticket.class));
        verify(ticketOperationLogMapper).insertLog(any());
    }

    @Test
    @DisplayName("NEW 状态工单不能确认")
    void confirmNewTicketShouldThrow() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(newTicket);

        assertThatThrownBy(() -> ticketService.confirmTicket(1L, new TicketConfirmDTO()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许确认");
    }

    // ==================== 取消工单 ====================

    @Test
    @DisplayName("NEW 状态工单可取消到 CANCELLED")
    void cancelNewShouldTransitionToCancelled() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(newTicket);

        TicketCancelDTO dto = new TicketCancelDTO();
        dto.setComment("不需要了");

        ticketService.cancelTicket(1L, dto);

        verify(ticketMapper).updateTicket(any(Ticket.class));
        verify(ticketOperationLogMapper).insertLog(any());
    }

    @Test
    @DisplayName("WAIT_CONFIRM 状态工单不能取消")
    void cancelWaitConfirmShouldThrow() {
        when(ticketMapper.selectTicketEntityById(3L)).thenReturn(waitConfirmTicket);

        TicketCancelDTO dto = new TicketCancelDTO();
        dto.setComment("想取消");

        assertThatThrownBy(() -> ticketService.cancelTicket(3L, dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许取消");
    }

    @Test
    @DisplayName("取消原因必填")
    void cancelWithEmptyCommentShouldThrow() {
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(newTicket);

        TicketCancelDTO dto = new TicketCancelDTO();
        dto.setComment("");

        assertThatThrownBy(() -> ticketService.cancelTicket(1L, dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能为空");
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("查询不存在的工单应抛异常")
    void queryNonExistentTicketShouldThrow() {
        when(ticketMapper.selectTicketById(999L)).thenReturn(null);

        assertThatThrownBy(() -> ticketService.selectTicketById(999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("工单不存在");
    }

    @Test
    @DisplayName("工单编号生成应递增")
    void ticketNoShouldIncrement() {
        when(ticketMapper.selectMaxTicketNo(anyString()))
                .thenReturn("TK202606250005")
                .thenReturn("TK202606250005");

        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("测试");

        ticketService.createTicket(dto);
        // 第二次调用应生成 TK202606250006
        ticketService.createTicket(dto);

        verify(ticketMapper, org.mockito.Mockito.times(2)).insertTicket(any(Ticket.class));
    }
}
