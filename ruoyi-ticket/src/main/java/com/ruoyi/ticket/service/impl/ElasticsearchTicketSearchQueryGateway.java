package com.ruoyi.ticket.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.model.TicketSearchCandidate;
import com.ruoyi.ticket.model.TicketSearchCandidatePage;
import com.ruoyi.ticket.service.TicketSearchQueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Elasticsearch 工单查询网关。 */
@Component
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class ElasticsearchTicketSearchQueryGateway implements TicketSearchQueryGateway {
    private static final String HIGHLIGHT_START = "\uE000";
    private static final String HIGHLIGHT_END = "\uE001";

    @Autowired private ElasticsearchClient elasticsearchClient;
    @Autowired private TicketSearchProperties properties;

    @Override
    public TicketSearchCandidatePage search(TicketSearchQueryDTO query, TicketAccessScope scope,
                                            List<String> searchAfter, int size) {
        try {
            SearchRequest request = buildRequest(query, scope, searchAfter, size + 1);
            SearchResponse<TicketSearchDocument> response = elasticsearchClient.search(
                    request, TicketSearchDocument.class);
            List<Hit<TicketSearchDocument>> hits = response.hits().hits();
            boolean hasMore = hits.size() > size;
            int resultSize = Math.min(size, hits.size());
            List<TicketSearchCandidate> candidates = new ArrayList<>(resultSize);
            for (int index = 0; index < resultSize; index++) {
                Hit<TicketSearchDocument> hit = hits.get(index);
                List<String> highlights = hit.highlight().values().stream().flatMap(List::stream).toList();
                candidates.add(new TicketSearchCandidate(hit.source(), highlights, encodeSortValues(hit.sort())));
            }
            return new TicketSearchCandidatePage(candidates, hasMore);
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch 工单查询失败", exception);
        }
    }

    private SearchRequest buildRequest(TicketSearchQueryDTO query, TicketAccessScope scope,
                                       List<String> searchAfter, int size) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(properties.getIndexAlias()).size(size).trackTotalHits(total -> total.enabled(false))
                .query(buildQuery(query, scope))
                .highlight(highlight -> highlight.preTags(HIGHLIGHT_START).postTags(HIGHLIGHT_END)
                        .fields("title", field -> field.fragmentSize(150))
                        .fields("content", field -> field.fragmentSize(150))
                        .fields("comments", field -> field.fragmentSize(150)));
        addSort(builder, query);
        if (searchAfter != null && !searchAfter.isEmpty()) {
            builder.searchAfter(searchAfter.stream().map(this::decodeSortValue).toList());
        }
        return builder.build();
    }

    private Query buildQuery(TicketSearchQueryDTO query, TicketAccessScope scope) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            bool.must(must -> must.multiMatch(match -> match.query(query.getKeyword())
                    .fields("ticketNo^4", "title^3", "content", "comments")));
        }
        addTerm(bool, "status", query.getStatus()); addTerm(bool, "priority", query.getPriority());
        addTerm(bool, "categoryId", query.getCategoryId()); addTerm(bool, "creatorId", query.getCreatorId());
        addTerm(bool, "assigneeId", query.getAssigneeId()); addTerm(bool, "deptId", query.getDeptId());
        addDateRange(bool, query.getBeginTime(), query.getEndTime());
        addAccessScope(bool, scope);
        return Query.of(queryBuilder -> queryBuilder.bool(bool.build()));
    }

    private void addTerm(BoolQuery.Builder bool, String field, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            if (value instanceof String stringValue) {
                bool.filter(filter -> filter.match(match -> match.field(field).query(stringValue)));
            } else {
                bool.filter(filter -> filter.term(term -> term.field(field).value(FieldValue.of(value))));
            }
        }
    }

    private void addDateRange(BoolQuery.Builder bool, Date beginTime, Date endTime) {
        if (beginTime == null && endTime == null) { return; }
        bool.filter(filter -> filter.range(range -> range.date(date -> {
            date.field("createTime");
            if (beginTime != null) { date.gte(String.valueOf(beginTime.getTime())); }
            if (endTime != null) { date.lte(String.valueOf(endTime.getTime())); }
            return date;
        })));
    }

    private void addAccessScope(BoolQuery.Builder bool, TicketAccessScope scope) {
        if (scope.isFullAccess() || scope.isIncludeDeptChildren() || !scope.getCustomRoleIds().isEmpty()) { return; }
        bool.filter(filter -> filter.bool(access -> {
            access.should(term("creatorId", scope.getUserId())).should(term("assigneeId", scope.getUserId()));
            if (scope.isIncludeDept()) { access.should(term("deptId", scope.getDeptId())); }
            return access.minimumShouldMatch("1");
        }));
    }

    private Query term(String field, Long value) {
        return Query.of(query -> query.term(term -> term.field(field).value(value)));
    }

    private void addSort(SearchRequest.Builder builder, TicketSearchQueryDTO query) {
        SortOrder order = "ASC".equals(query.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
        if ("CREATE_TIME".equals(query.getSortBy())) {
            builder.sort(sort -> sort.field(field -> field.field("createTime").order(order)));
        } else if ("UPDATE_TIME".equals(query.getSortBy())) {
            builder.sort(sort -> sort.field(field -> field.field("updateTime").order(order)));
        } else {
            builder.sort(sort -> sort.score(score -> score.order(SortOrder.Desc)));
        }
        builder.sort(sort -> sort.field(field -> field.field("ticketId").order(order)));
    }

    private List<String> encodeSortValues(List<FieldValue> values) {
        return values.stream().map(value -> {
            if (value.isLong()) { return "L:" + value.longValue(); }
            if (value.isDouble()) { return "D:" + value.doubleValue(); }
            return "S:" + value.stringValue();
        }).toList();
    }

    private FieldValue decodeSortValue(String value) {
        if (value.startsWith("L:")) { return FieldValue.of(Long.parseLong(value.substring(2))); }
        if (value.startsWith("D:")) { return FieldValue.of(Double.parseDouble(value.substring(2))); }
        if (value.startsWith("S:")) { return FieldValue.of(value.substring(2)); }
        throw new IllegalArgumentException("检索游标排序值无效");
    }
}
