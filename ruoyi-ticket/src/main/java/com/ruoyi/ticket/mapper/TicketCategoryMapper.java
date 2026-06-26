package com.ruoyi.ticket.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.ticket.domain.TicketCategory;

/**
 * 工单分类 Mapper 接口
 *
 * @author ticket
 */
public interface TicketCategoryMapper {

    /**
     * 查询分类列表
     */
    List<TicketCategory> selectCategoryList(TicketCategory category);

    /**
     * 查询所有正常分类（用于构建树）
     */
    List<TicketCategory> selectCategoryTree();

    /**
     * 根据 ID 查询分类
     */
    TicketCategory selectCategoryById(Long categoryId);

    /**
     * 新增分类
     */
    int insertCategory(TicketCategory category);

    /**
     * 更新分类
     */
    int updateCategory(TicketCategory category);

    /**
     * 删除分类（逻辑删除）
     */
    int deleteCategoryById(Long categoryId);

    /**
     * 查询指定父分类下的子分类数量
     */
    int countByParentId(Long parentId);

    /**
     * 检查同父分类下是否存在同名分类
     */
    int checkCategoryNameUnique(@Param("categoryName") String categoryName,
                                @Param("parentId") Long parentId,
                                @Param("categoryId") Long categoryId);

    /**
     * 批量更新子分类的 ancestors（父分类变更时级联更新）
     */
    int updateCategoryChildren(@Param("children") List<TicketCategory> children);
}
