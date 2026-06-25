package com.ruoyi.ticket.dto;

import java.io.Serializable;

/**
 * 处理工单请求体
 *
 * @author ticket
 */
public class TicketProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理备注 */
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
