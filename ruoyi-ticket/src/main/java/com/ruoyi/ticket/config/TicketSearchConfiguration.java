package com.ruoyi.ticket.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 工单 Elasticsearch 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TicketSearchProperties.class)
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchConfiguration {

    /**
     * 创建 Elasticsearch 传输层，容器关闭时同步释放底层连接。
     *
     * @param properties 工单检索配置
     * @return Elasticsearch 传输层
     */
    @Bean(destroyMethod = "close")
    public ElasticsearchTransport ticketSearchTransport(TicketSearchProperties properties) {
        HttpHost[] hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
        RestClient restClient = RestClient.builder(hosts).build();
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    /**
     * 创建工单检索客户端。
     *
     * @param transport Elasticsearch 传输层
     * @return Elasticsearch 客户端
     */
    @Bean
    public ElasticsearchClient ticketSearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /** 工单索引重建专用单线程执行器。 */
    @Bean
    public TaskExecutor ticketSearchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("ticket-search-rebuild-");
        executor.initialize();
        return executor;
    }
}
