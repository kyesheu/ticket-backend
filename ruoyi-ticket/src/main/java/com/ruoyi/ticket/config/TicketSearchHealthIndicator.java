package com.ruoyi.ticket.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工单 Elasticsearch 可降级投影健康明细。
 */
@Component("ticketSearch")
@ConditionalOnClass(HealthIndicator.class)
@EnableConfigurationProperties(TicketSearchProperties.class)
public class TicketSearchHealthIndicator implements HealthIndicator {

    private final TicketSearchProperties properties;
    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;

    public TicketSearchHealthIndicator(TicketSearchProperties properties,
                                       ObjectProvider<ElasticsearchClient> elasticsearchClientProvider) {
        this.properties = properties;
        this.elasticsearchClientProvider = elasticsearchClientProvider;
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
            ElasticsearchClient elasticsearchClient = elasticsearchClientProvider.getIfAvailable();
            if (elasticsearchClient == null) {
                return degraded("ticket_search_client_missing");
            }
            boolean available = elasticsearchClient.ping().value();
            return Health.up()
                    .withDetail("status", available ? "UP" : "DEGRADED")
                    .withDetail("required", false)
                    .withDetail("indexAlias", properties.getIndexAlias())
                    .build();
        } catch (RuntimeException exception) {
            return degraded("ticket_search_unavailable");
        } catch (IOException exception) {
            return degraded("ticket_search_unavailable");
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
