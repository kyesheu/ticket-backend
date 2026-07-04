package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 工单检索游标页。 */
public class TicketSearchResultVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private List<TicketSearchItemVO> items;
    private boolean hasMore;
    private String nextCursor;
    public List<TicketSearchItemVO> getItems() { return items; }
    public void setItems(List<TicketSearchItemVO> items) { this.items = items; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}
