package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.dto.TicketCommentDTO;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工单评论通知测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单评论通知测试")
class TicketCommentServiceImplTest {

    @Mock private TicketCommentMapper ticketCommentMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private ITicketNotificationService ticketNotificationService;
    @InjectMocks private TicketCommentServiceImpl ticketCommentService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(7L);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("评论后应通知创建人和指派人")
    void addCommentShouldNotifyParticipants() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setCreatorId(8L);
        ticket.setAssigneeId(9L);
        when(ticketMapper.selectTicketEntityById(1L)).thenReturn(ticket);
        when(ticketCommentMapper.insertComment(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setCommentId(11L);
            return 1;
        });
        TicketCommentDTO dto = new TicketCommentDTO();
        dto.setContent("请查看处理结果");

        ticketCommentService.addComment(1L, dto);

        verify(ticketNotificationService).createNotification(eq(1L), eq(8L), eq(7L),
                any(), eq("COMMENTED:11"), anyString(), anyString());
        verify(ticketNotificationService).createNotification(eq(1L), eq(9L), eq(7L),
                any(), eq("COMMENTED:11"), anyString(), anyString());
    }
}
