package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 服务接收结果。
 */
public class TicketAiAcceptedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean accepted;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
}
