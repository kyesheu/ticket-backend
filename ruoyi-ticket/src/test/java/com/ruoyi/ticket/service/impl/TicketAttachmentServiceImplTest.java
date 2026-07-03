package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketAttachment;
import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketAttachmentMapper;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAttachmentDownload;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketAttachmentStorage;
import com.ruoyi.ticket.service.ITicketAttachmentCleanupService;
import com.ruoyi.ticket.vo.TicketAttachmentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工单附件 Service 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单附件 Service 测试")
class TicketAttachmentServiceImplTest {
    private static final long TEN_MEGABYTES = 10L * 1024L * 1024L;

    @Mock
    private TicketAttachmentMapper attachmentMapper;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private ITicketAttachmentStorage attachmentStorage;

    @Mock
    private ITicketAttachmentCleanupService attachmentCleanupService;

    @Mock
    private ITicketAccessPolicy accessPolicy;

    private TicketAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketAttachmentServiceImpl();
        ReflectionTestUtils.setField(service, "ticketAttachmentMapper", attachmentMapper);
        ReflectionTestUtils.setField(service, "ticketMapper", ticketMapper);
        ReflectionTestUtils.setField(service, "ticketAttachmentStorage", attachmentStorage);
        ReflectionTestUtils.setField(service, "ticketAttachmentCleanupService", attachmentCleanupService);
        ReflectionTestUtils.setField(service, "ticketAccessPolicy", accessPolicy);
    }

    @Test
    void shouldUploadTemporaryAttachmentAndSaveMetadata() throws IOException {
        MultipartFile file = file("report.pdf", TEN_MEGABYTES, "application/pdf");
        when(attachmentStorage.store(file)).thenReturn("/profile/upload/2026/07/report_uuid.pdf");
        when(attachmentMapper.insertAttachment(any(TicketAttachment.class))).thenAnswer(invocation -> {
            TicketAttachment attachment = invocation.getArgument(0);
            attachment.setAttachmentId(12L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            security.when(SecurityUtils::getUsername).thenReturn("tester");

            TicketAttachmentVO result = service.uploadTemporary(file);

            assertThat(result.getAttachmentId()).isEqualTo(12L);
            assertThat(result.getOriginalName()).isEqualTo("report.pdf");
            assertThat(result.getFileExtension()).isEqualTo("pdf");
            assertThat(result.getFileSize()).isEqualTo(TEN_MEGABYTES);
            assertThat(result.getUploaderId()).isEqualTo(7L);
        }
    }

    @Test
    void shouldRejectEmptyAttachment() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.uploadTemporary(file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件不能为空");
    }

    @Test
    void shouldRejectAttachmentLargerThanLimit() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("large.pdf");
        when(file.getSize()).thenReturn(TEN_MEGABYTES + 1L);

        assertThatThrownBy(() -> service.uploadTemporary(file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件大小不能超过10MB");
    }

    @Test
    void shouldRejectPathStyleOriginalName() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("../secret.txt");

        assertThatThrownBy(() -> service.uploadTemporary(file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件文件名无效");
    }

    @Test
    void shouldRejectControlCharacterInOriginalName() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("report\r\n.pdf");

        assertThatThrownBy(() -> service.uploadTemporary(file))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件文件名无效");
    }

    @Test
    void shouldDeleteStoredFileWhenMetadataInsertFails() throws IOException {
        MultipartFile file = file("report.pdf", 10L, "application/pdf");
        String storagePath = "/profile/upload/2026/07/report_uuid.pdf";
        when(attachmentStorage.store(file)).thenReturn(storagePath);
        doThrow(new IllegalStateException("database error"))
                .when(attachmentMapper).insertAttachment(any(TicketAttachment.class));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            security.when(SecurityUtils::getUsername).thenReturn("tester");

            assertThatThrownBy(() -> service.uploadTemporary(file))
                    .isInstanceOf(IllegalStateException.class);
        }
        verify(attachmentStorage).delete(storagePath);
    }

    @Test
    void shouldDeleteStoredFileWhenMetadataInsertAffectsNoRows() throws IOException {
        MultipartFile file = file("report.pdf", 10L, "application/pdf");
        String storagePath = "/profile/upload/2026/07/report_uuid.pdf";
        when(attachmentStorage.store(file)).thenReturn(storagePath);
        when(attachmentMapper.insertAttachment(any(TicketAttachment.class))).thenReturn(0);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            security.when(SecurityUtils::getUsername).thenReturn("tester");

            assertThatThrownBy(() -> service.uploadTemporary(file))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("附件元数据保存失败");
        }
        verify(attachmentStorage).delete(storagePath);
    }

    @Test
    void shouldCheckTicketAccessBeforeListingAttachments() {
        TicketAttachment attachment = attachment(12L, 100L);
        when(attachmentMapper.selectByTicketId(100L)).thenReturn(List.of(attachment));

        List<TicketAttachmentVO> result = service.selectByTicketId(100L);

        verify(accessPolicy).assertCanAccess(100L, "ticket:attachment:list");
        assertThat(result).extracting(TicketAttachmentVO::getAttachmentId).containsExactly(12L);
    }

    @Test
    void shouldCheckTicketAccessAndLoadDownloadResource() {
        TicketAttachment attachment = attachment(12L, 100L);
        attachment.setOriginalName("report.pdf");
        attachment.setContentType("application/pdf");
        attachment.setStoragePath("/profile/upload/2026/07/report_uuid.pdf");
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(attachmentStorage.load(attachment.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1}));

        TicketAttachmentDownload result = service.loadForDownload(12L);

        verify(accessPolicy).assertCanAccess(100L, "ticket:attachment:download");
        assertThat(result.getOriginalName()).isEqualTo("report.pdf");
        assertThat(result.getResource()).isNotNull();
    }

    @Test
    void shouldRejectDownloadingUnboundAttachment() {
        when(attachmentMapper.selectById(12L)).thenReturn(attachment(12L, null));

        assertThatThrownBy(() -> service.loadForDownload(12L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件不存在");
    }

    @Test
    void shouldBindOwnedTemporaryAttachments() {
        List<Long> attachmentIds = List.of(11L, 12L);
        TicketAttachment first = attachment(11L, null);
        TicketAttachment second = attachment(12L, null);
        first.setUploaderId(7L);
        second.setUploaderId(7L);
        when(attachmentMapper.selectByIds(attachmentIds)).thenReturn(List.of(first, second));
        when(attachmentMapper.countByTicketId(100L)).thenReturn(2);
        when(attachmentMapper.bindAttachments(100L, "TICKET", 100L, 7L, attachmentIds)).thenReturn(2);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET, 100L, attachmentIds);
        }

        verify(attachmentMapper).bindAttachments(100L, "TICKET", 100L, 7L, attachmentIds);
    }

    @Test
    void shouldIgnoreEmptyAttachmentIds() {
        service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET, 100L, List.of());

        verifyNoInteractions(attachmentMapper);
    }

    @Test
    void shouldRejectDuplicateAttachmentIds() {
        assertThatThrownBy(() -> service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET,
                100L, List.of(11L, 11L)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件ID不能为空或重复");
    }

    @Test
    void shouldRejectAttachmentUploadedByAnotherUser() {
        TicketAttachment attachment = attachment(11L, null);
        attachment.setUploaderId(8L);
        when(attachmentMapper.selectByIds(List.of(11L))).thenReturn(List.of(attachment));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET,
                    100L, List.of(11L)))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("附件不存在或不可绑定");
        }
    }

    @Test
    void shouldRejectAttachmentAlreadyBound() {
        TicketAttachment attachment = attachment(11L, 99L);
        attachment.setUploaderId(7L);
        attachment.setBusinessType("TICKET");
        attachment.setBusinessId(99L);
        when(attachmentMapper.selectByIds(List.of(11L))).thenReturn(List.of(attachment));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET,
                    100L, List.of(11L)))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("附件不存在或不可绑定");
        }
    }

    @Test
    void shouldRejectMoreThanTwentyAttachmentsPerTicket() {
        TicketAttachment first = attachment(11L, null);
        TicketAttachment second = attachment(12L, null);
        first.setUploaderId(7L);
        second.setUploaderId(7L);
        when(attachmentMapper.selectByIds(List.of(11L, 12L))).thenReturn(List.of(first, second));
        when(attachmentMapper.countByTicketId(100L)).thenReturn(19);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> service.bindAttachments(100L, TicketAttachmentBusinessType.TICKET,
                    100L, List.of(11L, 12L)))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("每个工单最多绑定20个附件");
        }
    }

    @Test
    void shouldRejectConcurrentDuplicateBinding() {
        TicketAttachment attachment = attachment(11L, null);
        attachment.setUploaderId(7L);
        when(attachmentMapper.selectByIds(List.of(11L))).thenReturn(List.of(attachment));
        when(attachmentMapper.bindAttachments(100L, "COMMENT", 20L, 7L, List.of(11L))).thenReturn(0);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> service.bindAttachments(100L, TicketAttachmentBusinessType.COMMENT,
                    20L, List.of(11L)))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("附件绑定失败，请勿重复提交");
        }
    }

    @Test
    void shouldLogicallyDeleteOwnedAttachmentAndCleanStorage() {
        TicketAttachment attachment = deletableAttachment(12L, 100L, 7L);
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket(TicketStatus.PROCESSING));
        when(attachmentMapper.logicallyDelete(12L, "tester")).thenReturn(1);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            security.when(SecurityUtils::getUsername).thenReturn("tester");
            service.deleteAttachment(12L);
        }

        verify(accessPolicy).assertCanAccess(100L, "ticket:attachment:remove");
        verify(attachmentCleanupService).cleanup(12L, attachment.getStoragePath());
    }

    @Test
    void shouldRejectDeletingAttachmentUploadedByAnotherUser() {
        TicketAttachment attachment = deletableAttachment(12L, 100L, 8L);
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket(TicketStatus.PROCESSING));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> service.deleteAttachment(12L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("只能删除本人上传的附件");
        }
    }

    @Test
    void shouldRejectDeletingTerminalTicketAttachmentForNormalUser() {
        TicketAttachment attachment = deletableAttachment(12L, 100L, 7L);
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket(TicketStatus.CLOSED));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            assertThatThrownBy(() -> service.deleteAttachment(12L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("终态工单附件不可删除");
        }
    }

    @Test
    void shouldAllowAdministratorToDeleteTerminalTicketAttachment() {
        TicketAttachment attachment = deletableAttachment(12L, 100L, 8L);
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(ticketMapper.selectTicketEntityById(100L)).thenReturn(ticket(TicketStatus.CANCELLED));
        when(attachmentMapper.logicallyDelete(12L, "admin")).thenReturn(1);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            service.deleteAttachment(12L);
        }

        verify(attachmentMapper).logicallyDelete(12L, "admin");
    }

    @Test
    void shouldRejectRepeatedDelete() {
        when(attachmentMapper.selectById(12L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteAttachment(12L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件不存在");
    }

    @Test
    void shouldCleanPhysicalFileOnlyAfterTransactionCommit() {
        TicketAttachment attachment = deletableAttachment(12L, null, 7L);
        when(attachmentMapper.selectById(12L)).thenReturn(attachment);
        when(attachmentMapper.logicallyDelete(12L, "tester")).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            security.when(SecurityUtils::getUsername).thenReturn("tester");

            service.deleteAttachment(12L);
            verify(attachmentCleanupService, never()).cleanup(12L, attachment.getStoragePath());
            TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
            verify(attachmentCleanupService).cleanup(12L, attachment.getStoragePath());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private MultipartFile file(String originalName, long size, String contentType) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(originalName);
        when(file.getSize()).thenReturn(size);
        when(file.getContentType()).thenReturn(contentType);
        return file;
    }

    private TicketAttachment attachment(Long attachmentId, Long ticketId) {
        TicketAttachment attachment = new TicketAttachment();
        attachment.setAttachmentId(attachmentId);
        attachment.setTicketId(ticketId);
        return attachment;
    }

    private TicketAttachment deletableAttachment(Long attachmentId, Long ticketId, Long uploaderId) {
        TicketAttachment attachment = attachment(attachmentId, ticketId);
        attachment.setUploaderId(uploaderId);
        attachment.setStoragePath("/profile/upload/2026/07/report_uuid.pdf");
        return attachment;
    }

    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status.name());
        return ticket;
    }
}
