package com.ruoyi.ticket.model;

import com.ruoyi.ticket.domain.TicketSearchDocument;

import java.util.List;

/** Elasticsearch 返回的工单候选。 */
public record TicketSearchCandidate(TicketSearchDocument document, List<String> highlights,
                                    List<String> sortValues) {
}
