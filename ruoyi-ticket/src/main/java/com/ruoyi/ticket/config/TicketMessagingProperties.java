package com.ruoyi.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 工单异步消息配置。 */
@ConfigurationProperties(prefix = "ticket.messaging")
public class TicketMessagingProperties {

    /** 是否启用 RabbitMQ 异步投递。 */
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
