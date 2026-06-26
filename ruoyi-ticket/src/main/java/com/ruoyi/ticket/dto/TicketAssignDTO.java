package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 分派工单请求体
 *
 * @author ticket
 */
public class TicketAssignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 指派人ID */
    private Long assigneeId;

    /** 分派备注 */
    private String comment;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
