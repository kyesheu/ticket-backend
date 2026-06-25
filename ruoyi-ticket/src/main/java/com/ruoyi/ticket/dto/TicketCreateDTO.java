package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 创建工单请求体
 *
 * @author ticket
 */
public class TicketCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单标题 */
    private String title;

    /** 工单内容 */
    private String content;

    /** 分类ID */
    private Long categoryId;

    /** 优先级，默认 MEDIUM */
    private String priority;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
