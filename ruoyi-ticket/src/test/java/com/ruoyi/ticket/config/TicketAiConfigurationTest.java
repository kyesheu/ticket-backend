package com.ruoyi.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ticket.service.ITicketAiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单 AI 服务配置测试。
 */
@DisplayName("工单 AI 服务配置测试")
class TicketAiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(TicketAiConfiguration.class);

    @Test
    @DisplayName("默认关闭 AI 服务且不创建 HTTP adapter")
    void shouldDisableAiByDefault() {
        TicketAiProperties properties = new TicketAiProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseUrl()).isEqualTo("http://127.0.0.1:8090");
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ITicketAiService.class));
    }

    @Test
    @DisplayName("启用 AI 服务但缺少认证凭据时启动失败")
    void shouldRejectMissingServiceToken() {
        contextRunner.withPropertyValues("ticket.ai.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("启用 AI 服务并配置认证凭据时创建 HTTP adapter")
    void shouldCreateAdapterWhenEnabled() {
        contextRunner.withPropertyValues(
                        "ticket.ai.enabled=true",
                        "ticket.ai.service-token=test-service-token-12345")
                .run(context -> assertThat(context).hasSingleBean(ITicketAiService.class));
    }
}
