package com.ruoyi.ticket.service;

import java.util.List;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.vo.TicketCategoryTreeVO;

/**
 * 工单分类 Service 接口
 *
 * @author ticket
 */
public interface ITicketCategoryService {

    /**
     * 查询分类列表（平铺）
     */
    List<TicketCategory> selectCategoryList(TicketCategory category);

    /**
     * 查询分类树
     */
    List<TicketCategoryTreeVO> selectCategoryTree();

    /**
     * 根据 ID 查询分类
     */
    TicketCategory selectCategoryById(Long categoryId);

    /**
     * 新增分类
     */
    int insertCategory(TicketCategory category);

    /**
     * 修改分类
     */
    int updateCategory(TicketCategory category);

    /**
     * 删除分类（逻辑删除，有子节点时不允许删除）
     */
    int deleteCategoryById(Long categoryId);
}
