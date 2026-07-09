package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketCategory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface KnowledgeCategoryMapper {
    List<TicketCategory> selectCategoryList(TicketCategory category);
    List<TicketCategory> selectCategoryTree();
    TicketCategory selectCategoryById(Long categoryId);
    int insertCategory(TicketCategory category);
    int updateCategory(TicketCategory category);
    int deleteCategoryById(Long categoryId);
    int countByParentId(Long parentId);
    int checkCategoryNameUnique(@Param("categoryName") String categoryName,
                                @Param("parentId") Long parentId,
                                @Param("categoryId") Long categoryId);
    int updateCategoryChildren(@Param("children") List<TicketCategory> children);
}
