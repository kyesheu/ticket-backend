package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 添加评论请求体
 *
 * @author ticket
 */
public class TicketCommentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论内容 */
    private String content;

    /** 评论类型：INTERNAL 内部备注 / EXTERNAL 公开评论，默认 EXTERNAL */
    private String commentType;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommentType() {
        return commentType;
    }

    public void setCommentType(String commentType) {
        this.commentType = commentType;
    }
}
