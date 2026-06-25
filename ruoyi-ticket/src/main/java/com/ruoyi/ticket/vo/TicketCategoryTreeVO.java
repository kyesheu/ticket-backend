package com.ruoyi.ticket.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 工单分类树响应体
 *
 * @author ticket
 */
public class TicketCategoryTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private String ancestors;
    private Integer orderNum;
    private String status;

    /** 子分类列表 */
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
