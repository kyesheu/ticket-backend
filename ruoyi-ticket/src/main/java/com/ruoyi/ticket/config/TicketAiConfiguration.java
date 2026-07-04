package com.ruoyi.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.service.impl.HttpTicketAiServiceImpl;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 工单 AI 服务 HTTP 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TicketAiProperties.class)
@ConditionalOnProperty(prefix = "ticket.ai", name = "enabled", havingValue = "true")
public class TicketAiConfiguration {

    private static final int MIN_SERVICE_TOKEN_LENGTH = 16;

    private static final int MAX_RESPONSE_BYTES_LIMIT = 10 * 1024 * 1024;

    /**
     * 创建带连接超时的 JDK HTTP 客户端。
     *
     * @param properties AI 服务配置
     * @return JDK HTTP 客户端
     */
    @Bean
    public HttpClient ticketAiHttpClient(TicketAiProperties properties) {
        validate(properties);
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    /**
     * 创建工单 AI HTTP adapter。
     *
     * @param httpClient JDK HTTP 客户端
     * @param objectMapper JSON 序列化器
     * @param properties AI 服务配置
     * @return 工单 AI Service
     */
    @Bean
    public ITicketAiService ticketAiService(HttpClient httpClient, ObjectMapper objectMapper,
                                            TicketAiProperties properties) {
        return new HttpTicketAiServiceImpl(httpClient, objectMapper, properties);
    }

    private void validate(TicketAiProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("启用工单 AI 服务时必须配置服务地址");
        }
        if (!StringUtils.hasText(properties.getServiceToken())
                || properties.getServiceToken().length() < MIN_SERVICE_TOKEN_LENGTH) {
            throw new IllegalStateException("启用工单 AI 服务时必须配置服务间认证凭据");
        }
        if (properties.getConnectTimeout().isZero() || properties.getConnectTimeout().isNegative()
                || properties.getReadTimeout().isZero() || properties.getReadTimeout().isNegative()) {
            throw new IllegalStateException("AI 服务超时时间必须大于 0");
        }
        if (properties.getMaxResponseBytes() <= 0
                || properties.getMaxResponseBytes() > MAX_RESPONSE_BYTES_LIMIT) {
            throw new IllegalStateException("AI 服务最大响应字节数必须在 1 到 10485760 之间");
        }
    }
}
