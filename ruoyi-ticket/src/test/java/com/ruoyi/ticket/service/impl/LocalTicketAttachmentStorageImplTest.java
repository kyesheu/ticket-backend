package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地工单附件存储测试。
 */
class LocalTicketAttachmentStorageImplTest {
    @TempDir
    private Path tempDirectory;

    private String originalProfile;
    private LocalTicketAttachmentStorageImpl storage;

    @BeforeEach
    void setUp() {
        originalProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(tempDirectory.toString());
        storage = new LocalTicketAttachmentStorageImpl();
    }

    @AfterEach
    void tearDown() {
        new RuoYiConfig().setProfile(originalProfile);
    }

    @Test
    void shouldTreatMissingPhysicalFileAsCleaned() {
        assertThat(storage.delete("/profile/upload/missing.pdf")).isTrue();
    }

    @Test
    void shouldRejectPathEscapingUploadDirectory() {
        assertThatThrownBy(() -> storage.load("/profile/upload/../avatar/secret.png"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("附件文件路径无效");
    }
}
