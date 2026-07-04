package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已关闭工单知识同步请求。
 */
public class TicketAiClosedTicketSyncDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contractVersion = "v1";
    private Long ticketId;
    private String ticketNo;
    private String title;
    private String description;
    private String resolution;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
