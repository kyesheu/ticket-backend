package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.domain.TicketSearchDocument;
import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.model.TicketSearchCandidate;
import com.ruoyi.ticket.model.TicketSearchCandidatePage;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.TicketSearchCursorCodec;
import com.ruoyi.ticket.service.TicketSearchQueryGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** 工单全文检索 Service 测试。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单全文检索 Service 测试")
class TicketSearchServiceImplTest {
    @Mock private TicketSearchQueryGateway queryGateway;
    @Mock private TicketSearchCursorCodec cursorCodec;
    @Mock private ITicketAccessPolicy accessPolicy;
    @InjectMocks private TicketSearchServiceImpl searchService;

    @Test
    @DisplayName("批量权限复核后只返回可访问候选并转义高亮")
    void shouldReturnOnlyAccessibleCandidatesWithSafeHighlight() {
        TicketSearchQueryDTO query = new TicketSearchQueryDTO();
        query.setKeyword("login"); query.setPageSize(2);
        TicketAccessScope scope = new TicketAccessScope(7L, 3L, false, false, false, List.of());
        when(accessPolicy.resolveScope("ticket:search:query")).thenReturn(scope);
        when(queryGateway.search(eq(query), eq(scope), eq(null), eq(4))).thenReturn(
                new TicketSearchCandidatePage(List.of(candidate(1L, "<script>\uE000login\uE001", "L:1"),
                        candidate(2L, "ok", "L:2")), false));
        when(accessPolicy.filterAccessibleTicketIds(List.of(1L, 2L), "ticket:search:query"))
                .thenReturn(List.of(1L));

        var result = searchService.search(query);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getHighlights().getFirst())
                .isEqualTo("&lt;script&gt;<em>login</em>");
        assertThat(result.isHasMore()).isFalse();
    }

    @Test
    @DisplayName("拒绝超过上限的页大小")
    void shouldRejectOversizedPage() {
        TicketSearchQueryDTO query = new TicketSearchQueryDTO();
        query.setKeyword("login"); query.setPageSize(101);
        assertThatThrownBy(() -> searchService.search(query)).isInstanceOf(ServiceException.class)
                .hasMessageContaining("1 到 100");
    }

    private TicketSearchCandidate candidate(Long id, String highlight, String sort) {
        TicketSearchDocument document = new TicketSearchDocument();
        document.setTicketId(id); document.setTitle("title-" + id);
        return new TicketSearchCandidate(document, List.of(highlight), List.of(sort));
    }
}
