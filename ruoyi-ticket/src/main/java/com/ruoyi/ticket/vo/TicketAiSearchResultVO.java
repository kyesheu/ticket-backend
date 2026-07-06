package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 相似知识检索结果。
 */
public class TicketAiSearchResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<TicketAiSourceVO> sources;
    private Boolean degraded;
    private String reason;

    public List<TicketAiSourceVO> getSources() { return sources; }
    public void setSources(List<TicketAiSourceVO> sources) { this.sources = sources; }
    public Boolean getDegraded() { return degraded; }
    public void setDegraded(Boolean degraded) { this.degraded = degraded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
