package com.ruoyi.ticket.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ticket.domain.TicketCategory;
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

    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;

    @Override
    public List<TicketCategory> selectCategoryList(TicketCategory category) {
        // TODO: 阶段四实现
        return null;
    }

    @Override
    public List<TicketCategoryTreeVO> selectCategoryTree() {
        // TODO: 阶段四实现
        return null;
    }

    @Override
    public TicketCategory selectCategoryById(Long categoryId) {
        // TODO: 阶段四实现
        return null;
    }

    @Override
    public int insertCategory(TicketCategory category) {
        // TODO: 阶段四实现
        return 0;
    }

    @Override
    public int updateCategory(TicketCategory category) {
        // TODO: 阶段四实现
        return 0;
    }

    @Override
    public int deleteCategoryById(Long categoryId) {
        // TODO: 阶段四实现
        return 0;
    }
}
