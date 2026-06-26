package com.ruoyi.ticket.dto;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 处理工单请求体
 *
 * @author ticket
 */
@Schema(description = "处理工单请求")
public class TicketProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "处理备注", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
