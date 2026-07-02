package com.ruoyi.ticket.dto;
import java.io.Serial;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分类列表查询请求体
 *
 * @author ticket
 */
@Schema(description = "分类查询条件")
public class TicketCategoryQueryDTO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称（模糊匹配）", example = "网络")
    private String categoryName;

    @Schema(description = "状态：0正常 1停用", example = "0")
    private String status;

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
