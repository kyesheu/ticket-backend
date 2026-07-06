package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 历史工单同步响应。
 */
public class TicketAiClosedTicketSyncVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean accepted;
    private Long ticketId;
    private Long sourceGeneration;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getSourceGeneration() { return sourceGeneration; }
    public void setSourceGeneration(Long sourceGeneration) { this.sourceGeneration = sourceGeneration; }
}
