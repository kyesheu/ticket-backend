package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketNotification;
import com.ruoyi.ticket.dto.TicketNotificationQueryDTO;
import com.ruoyi.ticket.mapper.TicketNotificationMapper;
import com.ruoyi.ticket.messaging.TicketNotificationMessageDispatcher;
import com.ruoyi.ticket.vo.TicketNotificationVO;
import com.ruoyi.ticket.enums.TicketNotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 工单通知 Service 测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单通知 Service 测试")
class TicketNotificationServiceImplTest {

    @Mock
    private TicketNotificationMapper ticketNotificationMapper;

    @Mock
    private TicketNotificationMessageDispatcher messageDispatcher;

    @InjectMocks
    private TicketNotificationServiceImpl ticketNotificationService;

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
    @DisplayName("列表和未读数必须使用当前用户")
    void queryShouldUseCurrentUser() {
        TicketNotificationQueryDTO query = new TicketNotificationQueryDTO();
        when(ticketNotificationMapper.selectNotificationList(7L, query))
                .thenReturn(List.of(new TicketNotificationVO()));
        when(ticketNotificationMapper.countUnreadByRecipientId(7L)).thenReturn(3);

        assertThat(ticketNotificationService.selectMyNotifications(query)).hasSize(1);
        assertThat(ticketNotificationService.countMyUnread()).isEqualTo(3);
    }

    @Test
    @DisplayName("当前用户可标记自己的未读通知")
    void markUnreadNotificationShouldSucceed() {
        TicketNotification notification = new TicketNotification();
        notification.setReadStatus("0");
        when(ticketNotificationMapper.selectNotificationById(1L, 7L)).thenReturn(notification);

        ticketNotificationService.markRead(1L);

        verify(ticketNotificationMapper).markRead(1L, 7L);
    }

    @Test
    @DisplayName("重复标记已读应保持幂等")
    void markReadNotificationAgainShouldBeIdempotent() {
        TicketNotification notification = new TicketNotification();
        notification.setReadStatus("1");
        when(ticketNotificationMapper.selectNotificationById(1L, 7L)).thenReturn(notification);

        ticketNotificationService.markRead(1L);

        verify(ticketNotificationMapper, never()).markRead(1L, 7L);
    }

    @Test
    @DisplayName("他人通知对当前用户视为不存在")
    void markOtherUsersNotificationShouldThrow() {
        when(ticketNotificationMapper.selectNotificationById(2L, 7L)).thenReturn(null);

        assertThatThrownBy(() -> ticketNotificationService.markRead(2L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("全部已读只更新当前用户")
    void markAllReadShouldUseCurrentUser() {
        ticketNotificationService.markAllRead();
        verify(ticketNotificationMapper).markAllRead(7L);
    }

    @Test
    @DisplayName("通知接收人与操作人相同时不自发通知")
    void createNotificationShouldSkipOperator() {
        int rows = ticketNotificationService.createNotification(1L, 7L, 7L,
                TicketNotificationType.ASSIGNED, "ASSIGNED:1", "标题", "内容");

        assertThat(rows).isZero();
        verify(messageDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("通知接收人与操作人不同时应投递通知消息")
    void createNotificationShouldDispatchForRecipient() {
        int rows = ticketNotificationService.createNotification(1L, 8L, 7L,
                TicketNotificationType.ASSIGNED, "ASSIGNED:1", "标题", "内容");

        assertThat(rows).isOne();
        verify(messageDispatcher).dispatch(any());
    }
}
