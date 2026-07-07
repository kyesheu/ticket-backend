package com.ruoyi.ticket.config;

import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 工单 AI 可降级依赖健康明细。
 */
@Component("ticketAi")
@ConditionalOnClass(HealthIndicator.class)
@EnableConfigurationProperties(TicketAiProperties.class)
public class TicketAiHealthIndicator implements HealthIndicator {

    private final TicketAiProperties properties;
    private final ObjectProvider<ITicketAiService> ticketAiServiceProvider;

    public TicketAiHealthIndicator(TicketAiProperties properties,
                                   ObjectProvider<ITicketAiService> ticketAiServiceProvider) {
        this.properties = properties;
        this.ticketAiServiceProvider = ticketAiServiceProvider;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("status", "DISABLED")
                    .withDetail("required", false)
                    .build();
        }
        try {
            ITicketAiService ticketAiService = ticketAiServiceProvider.getIfAvailable();
            if (ticketAiService == null) {
                return degraded("ticket_ai_adapter_missing");
            }
            TicketAiHealthVO health = ticketAiService.health();
            return Health.up()
                    .withDetail("status", health.getStatus())
                    .withDetail("required", false)
                    .withDetail("contractVersion", health.getContractVersion())
                    .withDetail("elasticsearchAvailable", health.getElasticsearchAvailable())
                    .withDetail("embeddingConfigured", health.getEmbeddingConfigured())
                    .withDetail("llmConfigured", health.getLlmConfigured())
                    .build();
        } catch (RuntimeException exception) {
            return degraded("ticket_ai_unavailable");
        }
    }

    private Health degraded(String reason) {
        return Health.up()
                .withDetail("status", "DEGRADED")
                .withDetail("required", false)
                .withDetail("reason", reason)
                .build();
    }
}
