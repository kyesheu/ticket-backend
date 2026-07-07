package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 建议反馈记录。
 */
public class TicketAiFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long feedbackId;
    private Long ticketId;
    private String targetType;
    private Long targetId;
    private String feedbackValue;
    private Boolean adopted;
    private String comment;
    private Long evaluatorId;
    private Date createTime;

    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getFeedbackValue() { return feedbackValue; }
    public void setFeedbackValue(String feedbackValue) { this.feedbackValue = feedbackValue; }
    public Boolean getAdopted() { return adopted; }
    public void setAdopted(Boolean adopted) { this.adopted = adopted; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
