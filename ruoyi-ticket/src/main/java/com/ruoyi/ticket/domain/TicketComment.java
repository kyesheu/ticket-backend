package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单评论表实体
 *
 * @author ticket
 */
public class TicketComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long commentId;

    /** 工单ID */
    private Long ticketId;

    /** 评论人ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 评论类型：INTERNAL内部备注 EXTERNAL公开评论 */
    private String commentType;

    /** 删除标志：0存在 2删除 */
    private String delFlag;

    // ---- 展示字段 ----

    /** 评论人昵称 */
    private String userName;

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
