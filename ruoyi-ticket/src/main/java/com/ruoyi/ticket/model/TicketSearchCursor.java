package com.ruoyi.ticket.model;

import java.util.List;

/** 工单检索游标载荷。 */
public record TicketSearchCursor(String queryHash, List<String> sortValues) {
}
