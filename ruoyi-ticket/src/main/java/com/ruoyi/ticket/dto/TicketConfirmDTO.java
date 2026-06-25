package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 确认工单请求体
 *
 * @author ticket
 */
public class TicketConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 确认备注（可选） */
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
