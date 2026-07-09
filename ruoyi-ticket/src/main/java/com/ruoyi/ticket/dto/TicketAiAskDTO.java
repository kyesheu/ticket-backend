package com.ruoyi.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;

/**
 * AI 前置问答请求。
 */
@Schema(description = "AI 前置问答请求")
public class TicketAiAskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "问题不能为空")
    @Size(max = 10000, message = "问题不能超过10000个字符")
    @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Size(max = 100, message = "分类不能超过100个字符")
    @Schema(description = "用户选择的问题分类")
    private String category;

    @Schema(description = "检索数量")
    private Integer topK;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
