package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/** 工单检索结果项。 */
public class TicketSearchItemVO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long ticketId;
    private String ticketNo;
    private String title;
    private String priority;
    private String status;
    private Date createTime;
    private Date updateTime;
    private List<String> highlights;
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public List<String> getHighlights() { return highlights; }
    public void setHighlights(List<String> highlights) { this.highlights = highlights; }
}
