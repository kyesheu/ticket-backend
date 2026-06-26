package com.ruoyi.ticket.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.service.ITicketCategoryService;
import com.ruoyi.ticket.vo.TicketCategoryTreeVO;

/**
 * 工单分类 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketCategoryServiceImpl implements ITicketCategoryService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final String NORMAL_STATUS = "0";
    private static final String DISABLED_STATUS = "1";

    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;

    @Override
    public List<TicketCategory> selectCategoryList(TicketCategoryQueryDTO query) {
        TicketCategory category = new TicketCategory();
        if (query != null) {
            category.setCategoryName(query.getCategoryName());
            category.setStatus(query.getStatus());
        }
        return ticketCategoryMapper.selectCategoryList(category);
    }

    @Override
    public List<TicketCategoryTreeVO> selectCategoryTree() {
        List<TicketCategory> categoryList = ticketCategoryMapper.selectCategoryTree();
        List<TicketCategory> tree = buildCategoryTree(categoryList);
        return tree.stream().map(this::toTreeVO).collect(Collectors.toList());
    }

    @Override
    public TicketCategory selectCategoryById(Long categoryId) {
        return ticketCategoryMapper.selectCategoryById(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCategory(TicketCategoryCreateDTO dto) {
        TicketCategory category = new TicketCategory();
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID);
        category.setCategoryName(dto.getCategoryName());
        category.setOrderNum(dto.getOrderNum() != null ? dto.getOrderNum() : 0);

        // 校验父分类
        if (!ROOT_PARENT_ID.equals(category.getParentId())) {
            TicketCategory parent = ticketCategoryMapper.selectCategoryById(category.getParentId());
            if (parent == null) {
                throw new ServiceException("父分类不存在");
            }
            if (DISABLED_STATUS.equals(parent.getStatus())) {
                throw new ServiceException("父分类已停用，不允许新增子分类");
            }
            category.setAncestors(parent.getAncestors() + "," + category.getParentId());
        } else {
            category.setAncestors("0");
        }

        // 校验同父级下名称唯一
        if (!checkCategoryNameUnique(category.getCategoryName(), category.getParentId(), null)) {
            throw new ServiceException("同父级下已存在同名分类");
        }

        category.setStatus(StringUtils.isNotBlank(dto.getStatus()) ? dto.getStatus() : NORMAL_STATUS);
        category.setDelFlag("0");
        category.setCreateBy(SecurityUtils.getUsername());
        category.setCreateTime(new Date());
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(new Date());
        return ticketCategoryMapper.insertCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(TicketCategoryUpdateDTO dto) {
        TicketCategory existing = ticketCategoryMapper.selectCategoryById(dto.getCategoryId());
        if (existing == null) {
            throw new ServiceException("分类不存在");
        }

        TicketCategory category = new TicketCategory();
        category.setCategoryId(dto.getCategoryId());
        category.setParentId(dto.getParentId());
        category.setCategoryName(dto.getCategoryName());
        category.setOrderNum(dto.getOrderNum());
        category.setStatus(dto.getStatus());

        // 校验父分类
        if (!ROOT_PARENT_ID.equals(category.getParentId())) {
            TicketCategory parent = ticketCategoryMapper.selectCategoryById(category.getParentId());
            if (parent == null) {
                throw new ServiceException("父分类不存在");
            }
            if (category.getCategoryId().equals(parent.getCategoryId())) {
                throw new ServiceException("父分类不能是自己");
            }
            if (DISABLED_STATUS.equals(parent.getStatus())) {
                throw new ServiceException("父分类已停用，不允许移入");
            }
        }

        // 校验同父级下名称唯一
        if (!checkCategoryNameUnique(category.getCategoryName(), category.getParentId(), category.getCategoryId())) {
            throw new ServiceException("同父级下已存在同名分类");
        }

        String oldAncestors = existing.getAncestors();
        String newAncestors;
        if (ROOT_PARENT_ID.equals(category.getParentId())) {
            newAncestors = "0";
        } else {
            TicketCategory parent = ticketCategoryMapper.selectCategoryById(category.getParentId());
            newAncestors = parent.getAncestors() + "," + category.getParentId();
        }
        category.setAncestors(newAncestors);
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(new Date());
        int rows = ticketCategoryMapper.updateCategory(category);

        if (!newAncestors.equals(oldAncestors)) {
            updateChildrenAncestors(category.getCategoryId(), newAncestors, oldAncestors);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryById(Long categoryId) {
        TicketCategory category = ticketCategoryMapper.selectCategoryById(categoryId);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }
        int childCount = ticketCategoryMapper.countByParentId(categoryId);
        if (childCount > 0) {
            throw new ServiceException("该分类下存在子分类，无法删除");
        }
        return ticketCategoryMapper.deleteCategoryById(categoryId);
    }

    // ==================== 树构建 ====================

    private List<TicketCategory> buildCategoryTree(List<TicketCategory> categoryList) {
        List<TicketCategory> result = new ArrayList<>();
        List<Long> idList = categoryList.stream().map(TicketCategory::getCategoryId).collect(Collectors.toList());
        for (TicketCategory category : categoryList) {
            if (!idList.contains(category.getParentId())) {
                recursionFn(categoryList, category);
                result.add(category);
            }
        }
        if (result.isEmpty()) {
            result = categoryList;
        }
        return result;
    }

    private void recursionFn(List<TicketCategory> list, TicketCategory current) {
        List<TicketCategory> children = getChildren(list, current);
        current.setChildren(children);
        for (TicketCategory child : children) {
            if (hasChildren(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private List<TicketCategory> getChildren(List<TicketCategory> list, TicketCategory parent) {
        List<TicketCategory> result = new ArrayList<>();
        for (TicketCategory category : list) {
            if (category.getParentId() != null && category.getParentId().equals(parent.getCategoryId())) {
                result.add(category);
            }
        }
        return result;
    }

    private boolean hasChildren(List<TicketCategory> list, TicketCategory parent) {
        return !getChildren(list, parent).isEmpty();
    }

    private TicketCategoryTreeVO toTreeVO(TicketCategory category) {
        TicketCategoryTreeVO vo = new TicketCategoryTreeVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setParentId(category.getParentId());
        vo.setCategoryName(category.getCategoryName());
        vo.setAncestors(category.getAncestors());
        vo.setOrderNum(category.getOrderNum());
        vo.setStatus(category.getStatus());
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            vo.setChildren(category.getChildren().stream().map(this::toTreeVO).collect(Collectors.toList()));
        }
        return vo;
    }

    // ==================== TreeSelect ====================

    public List<TreeSelect> buildCategoryTreeSelect() {
        List<TicketCategory> categoryList = ticketCategoryMapper.selectCategoryTree();
        List<TicketCategory> tree = buildCategoryTree(categoryList);
        return tree.stream().map(this::toTreeSelect).collect(Collectors.toList());
    }

    private TreeSelect toTreeSelect(TicketCategory category) {
        TreeSelect ts = new TreeSelect();
        ts.setId(category.getCategoryId());
        ts.setLabel(category.getCategoryName());
        ts.setDisabled(DISABLED_STATUS.equals(category.getStatus()));
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            ts.setChildren(category.getChildren().stream().map(this::toTreeSelect).collect(Collectors.toList()));
        }
        return ts;
    }

    // ==================== 辅助方法 ====================

    private void updateChildrenAncestors(Long parentId, String newAncestors, String oldAncestors) {
        String oldPath = oldAncestors + "," + parentId;
        String newPath = newAncestors + "," + parentId;
        List<TicketCategory> children = ticketCategoryMapper.selectCategoryTree();
        List<TicketCategory> toUpdate = new ArrayList<>();
        for (TicketCategory child : children) {
            if (child.getAncestors() == null) {
                continue;
            }
            String childAncestors = child.getAncestors();
            if (childAncestors.equals(oldPath) || childAncestors.startsWith(oldPath + ",")) {
                child.setAncestors(newPath + childAncestors.substring(oldPath.length()));
                toUpdate.add(child);
            }
        }
        if (!toUpdate.isEmpty()) {
            ticketCategoryMapper.updateCategoryChildren(toUpdate);
        }
    }

    private boolean checkCategoryNameUnique(String name, Long parentId, Long categoryId) {
        int count = ticketCategoryMapper.checkCategoryNameUnique(name, parentId, categoryId);
        return count == 0;
    }
}
