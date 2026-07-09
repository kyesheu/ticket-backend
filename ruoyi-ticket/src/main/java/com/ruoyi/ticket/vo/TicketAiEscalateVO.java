package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 转人工建单结果。
 */
public class TicketAiEscalateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private Boolean autoAssigned;
    private String dispatchReason;
    private TicketAiTriageVO triage;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Boolean getAutoAssigned() { return autoAssigned; }
    public void setAutoAssigned(Boolean autoAssigned) { this.autoAssigned = autoAssigned; }
    public String getDispatchReason() { return dispatchReason; }
    public void setDispatchReason(String dispatchReason) { this.dispatchReason = dispatchReason; }
    public TicketAiTriageVO getTriage() { return triage; }
    public void setTriage(TicketAiTriageVO triage) { this.triage = triage; }
}
