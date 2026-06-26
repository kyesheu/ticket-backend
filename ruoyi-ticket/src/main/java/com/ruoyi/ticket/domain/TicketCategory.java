package com.ruoyi.ticket.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单分类表实体
 *
 * @author ticket
 */
@Schema(description = "工单分类")
public class TicketCategory extends BaseEntity {

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

    @Schema(description = "删除标志：0存在 2删除")
    private String delFlag;

    @Schema(description = "子分类列表")
    private List<TicketCategory> children;

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
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<TicketCategory> getChildren() { return children; }
    public void setChildren(List<TicketCategory> children) { this.children = children; }
}
