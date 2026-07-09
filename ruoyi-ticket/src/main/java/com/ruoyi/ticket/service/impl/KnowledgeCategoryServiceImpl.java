package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
import com.ruoyi.ticket.mapper.KnowledgeCategoryMapper;
import com.ruoyi.ticket.service.IKnowledgeCategoryService;
import com.ruoyi.ticket.vo.TicketCategoryTreeVO;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeCategoryServiceImpl implements IKnowledgeCategoryService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final String NORMAL_STATUS = "0";
    private static final String DISABLED_STATUS = "1";

    @Autowired
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Override
    public List<TicketCategory> selectCategoryList(TicketCategoryQueryDTO query) {
        TicketCategory category = new TicketCategory();
        if (query != null) {
            category.setCategoryName(query.getCategoryName());
            category.setStatus(query.getStatus());
        }
        return knowledgeCategoryMapper.selectCategoryList(category);
    }

    @Override
    public List<TicketCategoryTreeVO> selectCategoryTree() {
        return buildCategoryTree(knowledgeCategoryMapper.selectCategoryTree())
                .stream().map(this::toTreeVO).collect(Collectors.toList());
    }

    @Override
    public TicketCategory selectCategoryById(Long categoryId) {
        return knowledgeCategoryMapper.selectCategoryById(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCategory(TicketCategoryCreateDTO dto) {
        TicketCategory category = new TicketCategory();
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID);
        category.setCategoryName(dto.getCategoryName());
        category.setOrderNum(dto.getOrderNum() != null ? dto.getOrderNum() : 0);
        fillAncestors(category);
        ensureUnique(category.getCategoryName(), category.getParentId(), null);
        category.setStatus(StringUtils.isNotBlank(dto.getStatus()) ? dto.getStatus() : NORMAL_STATUS);
        category.setDelFlag("0");
        category.setCreateBy(SecurityUtils.getUsername());
        category.setCreateTime(new Date());
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(new Date());
        return knowledgeCategoryMapper.insertCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(TicketCategoryUpdateDTO dto) {
        TicketCategory existing = knowledgeCategoryMapper.selectCategoryById(dto.getCategoryId());
        if (existing == null) {
            throw new ServiceException("知识库分类不存在");
        }
        TicketCategory category = new TicketCategory();
        category.setCategoryId(dto.getCategoryId());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID);
        category.setCategoryName(dto.getCategoryName());
        category.setOrderNum(dto.getOrderNum() != null ? dto.getOrderNum() : 0);
        category.setStatus(StringUtils.isNotBlank(dto.getStatus()) ? dto.getStatus() : existing.getStatus());
        fillAncestors(category);
        ensureUnique(category.getCategoryName(), category.getParentId(), category.getCategoryId());
        category.setUpdateBy(SecurityUtils.getUsername());
        category.setUpdateTime(new Date());
        int rows = knowledgeCategoryMapper.updateCategory(category);
        if (!category.getAncestors().equals(existing.getAncestors())) {
            updateChildrenAncestors(category.getCategoryId(), category.getAncestors(), existing.getAncestors());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryById(Long categoryId) {
        if (knowledgeCategoryMapper.selectCategoryById(categoryId) == null) {
            throw new ServiceException("知识库分类不存在");
        }
        if (knowledgeCategoryMapper.countByParentId(categoryId) > 0) {
            throw new ServiceException("该分类下存在子分类，无法删除");
        }
        return knowledgeCategoryMapper.deleteCategoryById(categoryId);
    }

    private void fillAncestors(TicketCategory category) {
        if (ROOT_PARENT_ID.equals(category.getParentId())) {
            category.setAncestors("0");
            return;
        }
        TicketCategory parent = knowledgeCategoryMapper.selectCategoryById(category.getParentId());
        if (parent == null) {
            throw new ServiceException("父分类不存在");
        }
        if (category.getCategoryId() != null && category.getCategoryId().equals(parent.getCategoryId())) {
            throw new ServiceException("父分类不能是自己");
        }
        if (DISABLED_STATUS.equals(parent.getStatus())) {
            throw new ServiceException("父分类已停用");
        }
        category.setAncestors(parent.getAncestors() + "," + category.getParentId());
    }

    private void ensureUnique(String name, Long parentId, Long categoryId) {
        if (knowledgeCategoryMapper.checkCategoryNameUnique(name, parentId, categoryId) > 0) {
            throw new ServiceException("同父级下已存在同名分类");
        }
    }

    private List<TicketCategory> buildCategoryTree(List<TicketCategory> categoryList) {
        List<TicketCategory> result = new ArrayList<>();
        List<Long> idList = categoryList.stream().map(TicketCategory::getCategoryId).collect(Collectors.toList());
        for (TicketCategory category : categoryList) {
            if (!idList.contains(category.getParentId())) {
                recursionFn(categoryList, category);
                result.add(category);
            }
        }
        return result.isEmpty() ? categoryList : result;
    }

    private void recursionFn(List<TicketCategory> list, TicketCategory current) {
        List<TicketCategory> children = list.stream()
                .filter(item -> current.getCategoryId().equals(item.getParentId()))
                .collect(Collectors.toList());
        current.setChildren(children);
        children.forEach(child -> recursionFn(list, child));
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

    private void updateChildrenAncestors(Long parentId, String newAncestors, String oldAncestors) {
        String oldPath = oldAncestors + "," + parentId;
        String newPath = newAncestors + "," + parentId;
        List<TicketCategory> toUpdate = new ArrayList<>();
        for (TicketCategory child : knowledgeCategoryMapper.selectCategoryTree()) {
            String childAncestors = child.getAncestors();
            if (childAncestors != null && (childAncestors.equals(oldPath) || childAncestors.startsWith(oldPath + ","))) {
                child.setAncestors(newPath + childAncestors.substring(oldPath.length()));
                toUpdate.add(child);
            }
        }
        if (!toUpdate.isEmpty()) {
            knowledgeCategoryMapper.updateCategoryChildren(toUpdate);
        }
    }
}
