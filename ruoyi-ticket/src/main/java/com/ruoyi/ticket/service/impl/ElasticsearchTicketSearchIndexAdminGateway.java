package com.ruoyi.ticket.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.service.TicketSearchIndexAdminGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Elasticsearch 工单索引管理网关实现。 */
@Component
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class ElasticsearchTicketSearchIndexAdminGateway implements TicketSearchIndexAdminGateway {
    @Autowired private ElasticsearchClient elasticsearchClient;
    @Autowired private TicketSearchProperties properties;

    @Override
    public void createIndex(String indexName) {
        try {
            elasticsearchClient.indices().create(request -> request.index(indexName)
                    .settings(settings -> settings.numberOfShards("1").numberOfReplicas("0"))
                    .mappings(mapping -> mapping.dynamic(DynamicMapping.Strict)
                            .properties("ticketId", property -> property.long_(value -> value))
                            .properties("sourceEventId", property -> property.long_(value -> value))
                            .properties("ticketNo", property -> property.keyword(value -> value))
                            .properties("title", property -> property.text(value -> value.analyzer("standard")))
                            .properties("content", property -> property.text(value -> value.analyzer("standard")))
                            .properties("comments", property -> property.text(value -> value.analyzer("standard")))
                            .properties("categoryId", property -> property.long_(value -> value))
                            .properties("priority", property -> property.keyword(value -> value))
                            .properties("status", property -> property.keyword(value -> value))
                            .properties("creatorId", property -> property.long_(value -> value))
                            .properties("assigneeId", property -> property.long_(value -> value))
                            .properties("deptId", property -> property.long_(value -> value))
                            .properties("createTime", property -> property.date(value -> value.format("epoch_millis")))
                            .properties("updateTime", property -> property
                                    .date(value -> value.format("epoch_millis")))));
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 索引创建失败", exception);
        }
    }

    @Override
    public long countDocuments(String indexName) {
        try {
            return elasticsearchClient.count(request -> request.index(indexName)).count();
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 索引计数失败", exception);
        }
    }

    @Override
    public void refreshIndex(String indexName) {
        try {
            elasticsearchClient.indices().refresh(request -> request.index(indexName));
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 索引刷新失败", exception);
        }
    }

    @Override
    public void switchAlias(String indexName) {
        try {
            String indexAlias = properties.getIndexAlias();
            boolean aliasExists = elasticsearchClient.indices()
                    .existsAlias(request -> request.name(indexAlias)).value();
            boolean legacyIndexExists = !aliasExists && elasticsearchClient.indices()
                    .exists(request -> request.index(indexAlias)).value();
            UpdateAliasesRequest request = UpdateAliasesRequest.of(update -> {
                if (legacyIndexExists) {
                    update.actions(action -> action.removeIndex(remove -> remove.index(indexAlias)));
                } else {
                    update.actions(action -> action.remove(remove -> remove.index("*")
                            .alias(indexAlias).mustExist(false)));
                }
                return update.actions(action -> action.add(add -> add.index(indexName).alias(indexAlias)));
            });
            elasticsearchClient.indices().updateAliases(request);
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 别名切换失败", exception);
        }
    }
}
