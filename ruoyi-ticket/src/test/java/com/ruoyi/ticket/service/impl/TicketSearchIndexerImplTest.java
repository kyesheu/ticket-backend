package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.mapper.TicketCommentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.TicketSearchDocumentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单检索索引器测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单检索索引器测试")
class TicketSearchIndexerImplTest {

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketCommentMapper commentMapper;
    @Mock private TicketSearchDocumentGateway documentGateway;
    @InjectMocks private TicketSearchIndexerImpl indexer;

    @Test
    @DisplayName("从 MySQL 聚合工单和评论的最小检索投影")
    void shouldBuildSearchProjection() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(7L);
        ticket.setTicketNo("TK-7");
        ticket.setTitle("登录失败");
        ticket.setContent("无法登录后台");
        ticket.setCategoryId(2L);
        ticket.setStatus("PROCESSING");
        TicketComment comment = new TicketComment();
        comment.setContent("已重置密码");
        when(ticketMapper.selectTicketEntityById(7L)).thenReturn(ticket);
        when(commentMapper.selectCommentsByTicketId(7L)).thenReturn(List.of(comment));

        indexer.upsertTicket(7L, 31L);

        ArgumentCaptor<TicketSearchDocument> captor = ArgumentCaptor.forClass(TicketSearchDocument.class);
        verify(documentGateway).upsert(captor.capture());
        assertThat(captor.getValue().getSourceEventId()).isEqualTo(31L);
        assertThat(captor.getValue().getComments()).isEqualTo("已重置密码");
        assertThat(captor.getValue().getTitle()).isEqualTo("登录失败");
    }
}
