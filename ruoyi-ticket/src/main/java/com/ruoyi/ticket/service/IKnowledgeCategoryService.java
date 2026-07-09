package com.ruoyi.ticket.service;

import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
import com.ruoyi.ticket.vo.TicketCategoryTreeVO;
import java.util.List;

public interface IKnowledgeCategoryService {
    List<TicketCategory> selectCategoryList(TicketCategoryQueryDTO query);
    List<TicketCategoryTreeVO> selectCategoryTree();
    TicketCategory selectCategoryById(Long categoryId);
    int insertCategory(TicketCategoryCreateDTO dto);
    int updateCategory(TicketCategoryUpdateDTO dto);
    int deleteCategoryById(Long categoryId);
}
