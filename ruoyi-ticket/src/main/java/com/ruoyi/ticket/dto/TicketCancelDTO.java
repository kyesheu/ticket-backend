package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 取消工单请求体
 *
 * @author ticket
 */
public class TicketCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 取消原因 */
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
