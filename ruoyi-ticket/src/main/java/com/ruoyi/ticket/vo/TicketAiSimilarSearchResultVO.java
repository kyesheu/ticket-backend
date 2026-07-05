package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 历史工单相似检索响应。
 */
public class TicketAiSimilarSearchResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<TicketAiSimilarTicketVO> results;

    public List<TicketAiSimilarTicketVO> getResults() { return results; }
    public void setResults(List<TicketAiSimilarTicketVO> results) { this.results = results; }
}
