package com.ruoyi.ticket.dto;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改分类请求体
 *
 * @author ticket
 */
@Schema(description = "修改分类请求")
public class TicketCategoryUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "父分类ID，0为根节点", example = "1")
    private Long parentId;

    @Schema(description = "分类名称", example = "网络问题")
    private String categoryName;

    @Schema(description = "排序", example = "1")
    private Integer orderNum;

    @Schema(description = "状态：0正常 1停用", example = "0")
    private String status;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
