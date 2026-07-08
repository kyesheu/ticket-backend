package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AI 反馈提交请求。
 */
public class TicketAiFeedbackDTO {

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotBlank(message = "反馈目标类型不能为空")
    private String targetType;

    @NotNull(message = "反馈目标ID不能为空")
    private Long targetId;

    @NotBlank(message = "反馈值不能为空")
    private String feedbackValue;

    @NotNull(message = "采纳状态不能为空")
    private Boolean adopted;

    @Size(max = 500, message = "反馈备注不能超过500个字符")
    private String comment;

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
}
