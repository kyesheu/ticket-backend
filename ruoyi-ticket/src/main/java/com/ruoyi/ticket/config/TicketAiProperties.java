package com.ruoyi.ticket.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工单 AI 服务 HTTP 调用配置。
 */
@ConfigurationProperties(prefix = "ticket.ai")
public class TicketAiProperties {

    /** 单次允许读取的默认最大响应字节数。 */
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;

    /** 是否启用工单 AI 服务。 */
    private boolean enabled;

    /** Python FastAPI 服务地址。 */
    private String baseUrl = "http://127.0.0.1:8090";

    /** 服务间认证凭据，仅从外部配置读取。 */
    private String serviceToken;

    /** 建立 HTTP 连接的超时时间。 */
    private Duration connectTimeout = Duration.ofSeconds(2L);

    /** 单次请求的读取超时时间。 */
    private Duration readTimeout = Duration.ofSeconds(10L);

    /** 单次允许读取的最大响应字节数。 */
    private int maxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;

    /** 单个知识文档允许的最大字节数。 */
    private int maxDocumentBytes = 10 * 1024 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(int maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public int getMaxDocumentBytes() {
        return maxDocumentBytes;
    }

    public void setMaxDocumentBytes(int maxDocumentBytes) {
        this.maxDocumentBytes = maxDocumentBytes;
    }
}
