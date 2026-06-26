package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单分类表实体
 *
 * @author ticket
 */
public class TicketCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long categoryId;

    /** 父分类ID，0为根节点 */
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 祖级路径，如 0,1,5 */
    private String ancestors;

    /** 排序 */
    private Integer orderNum;

    /** 状态：0正常 1停用 */
    private String status;

    /** 删除标志：0存在 2删除 */
    private String delFlag;

    // ---- 展示字段 ----

    /** 子分类列表 */
    private java.util.List<TicketCategory> children;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public java.util.List<TicketCategory> getChildren() {
        return children;
    }

    public void setChildren(java.util.List<TicketCategory> children) {
        this.children = children;
    }
}
