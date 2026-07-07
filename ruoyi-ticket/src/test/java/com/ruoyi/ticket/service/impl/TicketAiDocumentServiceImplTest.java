package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.config.TicketAiProperties;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.service.ITicketAiService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 知识文档导入 Service 测试。
 */
@DisplayName("知识文档导入 Service 测试")
class TicketAiDocumentServiceImplTest {

    private ITicketAiService ticketAiService;
    private TicketAiDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        ticketAiService = mock(ITicketAiService.class);
        TicketAiProperties properties = new TicketAiProperties();
        properties.setMaxDocumentBytes(16);
        service = new TicketAiDocumentServiceImpl(ticketAiService, properties);
    }

    @Test
    @DisplayName("合法 UTF-8 文档编码后转发 Python")
    void shouldForwardValidDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "redis.md", "text/markdown", "缓存穿透".getBytes(StandardCharsets.UTF_8));

        service.importDocument("doc_1", file);

        ArgumentCaptor<TicketAiDocumentImportDTO> captor = ArgumentCaptor.forClass(TicketAiDocumentImportDTO.class);
        verify(ticketAiService).importDocument(captor.capture());
        assertThat(captor.getValue().getSourceId()).isEqualTo("doc_1");
        assertThat(captor.getValue().getContentBase64()).isEqualTo("57yT5a2Y56m/6YCP");
    }

    @Test
    @DisplayName("空文件和非法来源标识被拒绝")
    void shouldRejectInvalidInput() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.importDocument("bad/id", empty)).isInstanceOf(ServiceException.class);
        verify(ticketAiService, never()).importDocument(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("扩展名与 MIME 不匹配时拒绝")
    void shouldRejectMismatchedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.importDocument("doc-1", file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("知识文档类型不支持或 MIME 不匹配");
    }

    @Test
    @DisplayName("超过大小限制时拒绝")
    void shouldRejectOversizedDocument() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[17]);

        assertThatThrownBy(() -> service.importDocument("doc-1", file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("知识文档大小超过限制");
    }

    @Test
    @DisplayName("合法分页查询转发 Python 文档管理契约")
    void shouldForwardDocumentListQuery() {
        TicketAiDocumentQueryDTO query = new TicketAiDocumentQueryDTO();
        query.setPageNum(2);
        query.setPageSize(20);
        query.setStatus("ACTIVE");

        service.listDocuments(query);

        ArgumentCaptor<TicketAiDocumentQueryDTO> captor = ArgumentCaptor.forClass(TicketAiDocumentQueryDTO.class);
        verify(ticketAiService).listDocuments(captor.capture());
        assertThat(captor.getValue().getPageNum()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("分页边界非法时拒绝")
    void shouldRejectInvalidDocumentListPagination() {
        TicketAiDocumentQueryDTO query = new TicketAiDocumentQueryDTO();
        query.setPageNum(0);
        query.setPageSize(101);

        assertThatThrownBy(() -> service.listDocuments(query))
                .isInstanceOf(ServiceException.class)
                .hasMessage("知识文档页码必须大于 0");
        verify(ticketAiService, never()).listDocuments(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("文档详情校验 sourceId 后转发")
    void shouldForwardDocumentDetailQuery() {
        service.getDocument("doc-1");

        verify(ticketAiService).getDocument("doc-1");
    }
}
