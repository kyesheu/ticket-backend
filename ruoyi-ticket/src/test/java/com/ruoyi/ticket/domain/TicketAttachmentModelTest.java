package com.ruoyi.ticket.domain;

import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单附件基础模型测试。
 */
@DisplayName("工单附件基础模型测试")
class TicketAttachmentModelTest {

    @Test
    void shouldSupportTicketAndCommentBusinessTypes() {
        assertThat(TicketAttachmentBusinessType.values()).containsExactly(
                TicketAttachmentBusinessType.TICKET,
                TicketAttachmentBusinessType.COMMENT);
    }

    @Test
    void shouldMakeAttachmentSerializable() {
        assertThat(new TicketAttachment()).isInstanceOf(Serializable.class);
    }

    @Test
    void shouldKeepAttachmentMetadataSnapshot() {
        TicketAttachment attachment = new TicketAttachment();
        attachment.setOriginalName("error-log.txt");
        attachment.setStoragePath("/ticket/2026/07/error-log_abcd.txt");
        attachment.setFileSize(1024L);

        assertThat(attachment.getOriginalName()).isEqualTo("error-log.txt");
        assertThat(attachment.getStoragePath()).isEqualTo("/ticket/2026/07/error-log_abcd.txt");
        assertThat(attachment.getFileSize()).isEqualTo(1024L);
    }

    @Test
    void shouldAllowUnboundTemporaryAttachment() {
        TicketAttachment attachment = new TicketAttachment();

        assertThat(attachment.getTicketId()).isNull();
        assertThat(attachment.getBusinessType()).isNull();
        assertThat(attachment.getBusinessId()).isNull();
    }
}
