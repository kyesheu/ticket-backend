package com.ruoyi.ticket.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 工单满意度评价实体。 */
public class TicketSatisfaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long satisfactionId;
    private Long ticketId;
    private Long evaluatorId;
    private Integer score;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getSatisfactionId() { return satisfactionId; }
    public void setSatisfactionId(Long satisfactionId) { this.satisfactionId = satisfactionId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
