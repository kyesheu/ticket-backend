package com.ruoyi.ticket.dto;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 确认工单请求体
 *
 * @author ticket
 */
@Schema(description = "确认工单请求")
public class TicketConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "确认备注（可选）")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
