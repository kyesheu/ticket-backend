package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.mapper.TicketAttachmentMapper;
import com.ruoyi.ticket.service.ITicketAttachmentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单附件物理清理 Service 测试。
 */
@ExtendWith(MockitoExtension.class)
class TicketAttachmentCleanupServiceImplTest {
    private static final String STORAGE_PATH = "/profile/upload/2026/07/report_uuid.pdf";

    @Mock
    private TicketAttachmentMapper attachmentMapper;

    @Mock
    private ITicketAttachmentStorage attachmentStorage;

    private TicketAttachmentCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketAttachmentCleanupServiceImpl();
        ReflectionTestUtils.setField(service, "ticketAttachmentMapper", attachmentMapper);
        ReflectionTestUtils.setField(service, "ticketAttachmentStorage", attachmentStorage);
    }

    @Test
    void shouldMarkStorageDeletedAfterPhysicalCleanup() {
        when(attachmentStorage.delete(STORAGE_PATH)).thenReturn(true);
        when(attachmentMapper.markStorageDeleted(12L)).thenReturn(1);

        service.cleanup(12L, STORAGE_PATH);

        verify(attachmentMapper).markStorageDeleted(12L);
    }

    @Test
    void shouldKeepCleanupPendingWhenPhysicalDeleteFails() {
        when(attachmentStorage.delete(STORAGE_PATH)).thenReturn(false);

        service.cleanup(12L, STORAGE_PATH);

        verify(attachmentMapper, never()).markStorageDeleted(12L);
    }
}
