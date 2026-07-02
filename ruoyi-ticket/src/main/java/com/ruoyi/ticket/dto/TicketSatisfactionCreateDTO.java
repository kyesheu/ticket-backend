package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/** 满意度评价提交参数。 */
public class TicketSatisfactionCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分必须在 1 到 5 之间")
    @Max(value = 5, message = "评分必须在 1 到 5 之间")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过 500 个字符")
    private String content;

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
