package com.ruoyi.ticket.service;

import com.ruoyi.ticket.model.TicketSearchCursor;

/** 工单检索游标签名编解码器。 */
public interface TicketSearchCursorCodec {
    String encode(TicketSearchCursor cursor);
    TicketSearchCursor decode(String encodedCursor);
}
