package com.ruoyi.ticket.controller;

import com.ruoyi.ticket.dto.TicketSearchQueryDTO;
import com.ruoyi.ticket.service.ITicketSearchService;
import com.ruoyi.ticket.service.ITicketSearchRebuildService;
import com.ruoyi.ticket.vo.TicketSearchResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 工单检索 Controller 测试。 */
@ExtendWith(MockitoExtension.class)
class TicketSearchControllerTest {
    @Mock private ITicketSearchService ticketSearchService;
    @Mock private ITicketSearchRebuildService ticketSearchRebuildService;
    @InjectMocks private TicketSearchController controller;

    @Test
    void shouldReturnCursorResult() {
        TicketSearchQueryDTO query = new TicketSearchQueryDTO();
        TicketSearchResultVO result = new TicketSearchResultVO();
        result.setHasMore(true);
        when(ticketSearchService.search(query)).thenReturn(result);

        assertThat(controller.search(query).get("data")).isSameAs(result);
    }

    @Test
    void shouldStartRebuild() {
        assertThat(controller.rebuild().get("code")).isEqualTo(200);
        org.mockito.Mockito.verify(ticketSearchRebuildService).startRebuild();
    }
}
