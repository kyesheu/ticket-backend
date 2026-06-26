package com.ruoyi.ticket.service;

import java.util.List;
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
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
    List<TicketCategory> selectCategoryList(TicketCategoryQueryDTO query);

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
    int insertCategory(TicketCategoryCreateDTO dto);

    /**
     * 修改分类
     */
    int updateCategory(TicketCategoryUpdateDTO dto);

    /**
     * 删除分类
     */
    int deleteCategoryById(Long categoryId);
}
