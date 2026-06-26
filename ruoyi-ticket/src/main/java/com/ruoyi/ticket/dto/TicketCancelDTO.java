package com.ruoyi.ticket.dto;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 取消工单请求体
 *
 * @author ticket
 */
@Schema(description = "取消工单请求")
public class TicketCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "取消原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
