package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.enums.TicketPriority;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.model.TicketSearchCandidate;
import com.ruoyi.ticket.model.TicketSearchCandidatePage;
import com.ruoyi.ticket.model.TicketSearchCursor;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketSearchService;
import com.ruoyi.ticket.service.TicketSearchCursorCodec;
import com.ruoyi.ticket.service.TicketSearchQueryGateway;
import com.ruoyi.ticket.vo.TicketSearchItemVO;
import com.ruoyi.ticket.vo.TicketSearchResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** 工单全文检索服务实现。 */
@Service
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class TicketSearchServiceImpl implements ITicketSearchService {
    private static final String SEARCH_PERMISSION = "ticket:search:query";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_FETCH_ROUNDS = 20;
    private static final String HIGHLIGHT_START = "\uE000";
    private static final String HIGHLIGHT_END = "\uE001";
    private static final Set<String> SORT_FIELDS = Set.of("RELEVANCE", "CREATE_TIME", "UPDATE_TIME");
    private static final Set<String> SORT_ORDERS = Set.of("ASC", "DESC");

    @Autowired private TicketSearchQueryGateway queryGateway;
    @Autowired private TicketSearchCursorCodec cursorCodec;
    @Autowired private ITicketAccessPolicy accessPolicy;

    @Override
    public TicketSearchResultVO search(TicketSearchQueryDTO query) {
        normalizeAndValidate(query);
        String queryHash = fingerprint(query);
        List<String> searchAfter = decodeCursor(query.getCursor(), queryHash);
        TicketAccessScope scope = accessPolicy.resolveScope(SEARCH_PERMISSION);
        int pageSize = query.getPageSize();
        List<TicketSearchItemVO> items = new ArrayList<>(pageSize);
        boolean hasMore = false;
        List<String> lastScannedSort = searchAfter;

        for (int round = 0; round < MAX_FETCH_ROUNDS && items.size() < pageSize; round++) {
            int fetchSize = Math.min(MAX_PAGE_SIZE, Math.max(pageSize * 2, pageSize + 1));
            TicketSearchCandidatePage page;
            try {
                page = queryGateway.search(query, scope, lastScannedSort, fetchSize);
            } catch (RuntimeException exception) {
                throw new ServiceException("工单检索服务暂不可用");
            }
            if (page.candidates().isEmpty()) {
                hasMore = false;
                break;
            }
            List<Long> candidateIds = page.candidates().stream()
                    .map(candidate -> candidate.document().getTicketId()).toList();
            Set<Long> accessibleIds = new HashSet<>(
                    accessPolicy.filterAccessibleTicketIds(candidateIds, SEARCH_PERMISSION));
            for (int index = 0; index < page.candidates().size(); index++) {
                TicketSearchCandidate candidate = page.candidates().get(index);
                lastScannedSort = candidate.sortValues();
                if (accessibleIds.contains(candidate.document().getTicketId())) {
                    items.add(toItem(candidate));
                }
                if (items.size() == pageSize) {
                    hasMore = index < page.candidates().size() - 1 || page.hasMore();
                    break;
                }
            }
            if (items.size() == pageSize || !page.hasMore()) {
                break;
            }
        }

        TicketSearchResultVO result = new TicketSearchResultVO();
        result.setItems(items);
        result.setHasMore(hasMore);
        if (hasMore && lastScannedSort != null) {
            result.setNextCursor(cursorCodec.encode(new TicketSearchCursor(queryHash, lastScannedSort)));
        }
        return result;
    }

    private void normalizeAndValidate(TicketSearchQueryDTO query) {
        if (query == null) {
            throw new ServiceException("检索条件不能为空");
        }
        query.setPageSize(query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize());
        if (query.getPageSize() < 1 || query.getPageSize() > MAX_PAGE_SIZE) {
            throw new ServiceException("每页条数必须在 1 到 100 之间");
        }
        query.setSortBy(StringUtils.isBlank(query.getSortBy()) ? "RELEVANCE" : query.getSortBy().toUpperCase());
        query.setSortOrder(StringUtils.isBlank(query.getSortOrder()) ? "DESC" : query.getSortOrder().toUpperCase());
        if (!SORT_FIELDS.contains(query.getSortBy()) || !SORT_ORDERS.contains(query.getSortOrder())) {
            throw new ServiceException("检索排序参数无效");
        }
        validateEnum(query.getStatus(), TicketStatus.class, "工单状态无效");
        validateEnum(query.getPriority(), TicketPriority.class, "工单优先级无效");
        if (query.getBeginTime() != null && query.getEndTime() != null
                && query.getBeginTime().after(query.getEndTime())) {
            throw new ServiceException("开始时间不能晚于结束时间");
        }
        if (StringUtils.isBlank(query.getKeyword()) && !hasFilter(query)) {
            throw new ServiceException("关键词和过滤条件不能同时为空");
        }
    }

    private boolean hasFilter(TicketSearchQueryDTO query) {
        return StringUtils.isNotBlank(query.getStatus()) || StringUtils.isNotBlank(query.getPriority())
                || query.getCategoryId() != null || query.getCreatorId() != null || query.getAssigneeId() != null
                || query.getDeptId() != null || query.getBeginTime() != null || query.getEndTime() != null;
    }

    private <E extends Enum<E>> void validateEnum(String value, Class<E> enumType, String message) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(message);
        }
    }

    private List<String> decodeCursor(String encodedCursor, String queryHash) {
        if (StringUtils.isBlank(encodedCursor)) { return null; }
        TicketSearchCursor cursor = cursorCodec.decode(encodedCursor);
        if (!queryHash.equals(cursor.queryHash())) { throw new ServiceException("检索游标与查询条件不匹配"); }
        return cursor.sortValues();
    }

    private String fingerprint(TicketSearchQueryDTO query) {
        String canonical = String.join("|", value(query.getKeyword()), value(query.getStatus()),
                value(query.getPriority()), value(query.getCategoryId()), value(query.getCreatorId()),
                value(query.getAssigneeId()), value(query.getDeptId()), value(query.getBeginTime()),
                value(query.getEndTime()), query.getSortBy(), query.getSortOrder(), value(query.getPageSize()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String value(Object value) {
        if (value instanceof Date date) { return String.valueOf(date.getTime()); }
        return value == null ? "" : String.valueOf(value);
    }

    private TicketSearchItemVO toItem(TicketSearchCandidate candidate) {
        TicketSearchDocument document = candidate.document();
        TicketSearchItemVO item = new TicketSearchItemVO();
        item.setTicketId(document.getTicketId()); item.setTicketNo(document.getTicketNo());
        item.setTitle(document.getTitle());
        item.setPriority(document.getPriority());
        item.setStatus(document.getStatus());
        item.setCreateTime(document.getCreateTime()); item.setUpdateTime(document.getUpdateTime());
        item.setHighlights(candidate.highlights().stream().map(this::sanitizeHighlight).toList());
        return item;
    }

    private String sanitizeHighlight(String fragment) {
        String escaped = HtmlUtils.htmlEscape(fragment == null ? "" : fragment);
        return escaped.replace(HIGHLIGHT_START, "<em>").replace(HIGHLIGHT_END, "</em>");
    }
}
