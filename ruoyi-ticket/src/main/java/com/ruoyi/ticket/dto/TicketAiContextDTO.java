package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送给 Python AI 服务的最小工单上下文。
 */
public class TicketAiContextDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contractVersion = "v1";
    private String ticketNo;
    private String title;
    private String description;
    private String categoryName;
    private String priority;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
