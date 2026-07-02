package com.ruoyi.ticket.dto;
import java.io.Serial;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 添加评论请求体
 *
 * @author ticket
 */
@Schema(description = "添加评论请求")
public class TicketCommentDTO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "评论类型：INTERNAL 内部备注 / EXTERNAL 公开评论，默认 EXTERNAL")
    private String commentType;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCommentType() { return commentType; }
    public void setCommentType(String commentType) { this.commentType = commentType; }
}
