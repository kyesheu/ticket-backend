package com.ruoyi.ticket.model;

import java.util.List;

/** Elasticsearch 候选游标页。 */
public record TicketSearchCandidatePage(List<TicketSearchCandidate> candidates, boolean hasMore) {
}
