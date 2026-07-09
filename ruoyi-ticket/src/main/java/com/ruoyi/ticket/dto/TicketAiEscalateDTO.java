package com.ruoyi.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 问答转人工建单请求。
 */
@Schema(description = "AI 问答转人工建单请求")
public class TicketAiEscalateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "问题不能为空")
    @Size(max = 10000, message = "问题不能超过10000个字符")
    private String question;

    @Size(max = 10000, message = "AI回答不能超过10000个字符")
    private String aiAnswer;

    @Size(max = 4000, message = "AI建议不能超过4000个字符")
    private String aiSuggestion;

    @Size(max = 2000, message = "补充说明不能超过2000个字符")
    private String userComment;

    @NotNull(message = "工单分类不能为空")
    private Long categoryId;

    private String categoryName;

    private String priority;

    private List<Long> attachmentIds;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAiAnswer() { return aiAnswer; }
    public void setAiAnswer(String aiAnswer) { this.aiAnswer = aiAnswer; }
    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
    public String getUserComment() { return userComment; }
    public void setUserComment(String userComment) { this.userComment = userComment; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
}
