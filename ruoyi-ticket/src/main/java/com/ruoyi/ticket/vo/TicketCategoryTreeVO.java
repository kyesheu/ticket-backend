package com.ruoyi.ticket.vo;
import java.io.Serial;

import java.io.Serializable;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单分类树响应体
 *
 * @author ticket
 */
@Schema(description = "工单分类树节点")
public class TicketCategoryTreeVO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "父分类ID，0为根节点")
    private Long parentId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "祖级路径，如 0,1,5")
    private String ancestors;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "状态：0正常 1停用")
    private String status;

    @Schema(description = "子分类列表")
    private List<TicketCategoryTreeVO> children;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getAncestors() { return ancestors; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<TicketCategoryTreeVO> getChildren() { return children; }
    public void setChildren(List<TicketCategoryTreeVO> children) { this.children = children; }
}
