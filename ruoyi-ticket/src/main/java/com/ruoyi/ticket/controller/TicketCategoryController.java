package com.ruoyi.ticket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.service.ITicketCategoryService;

/**
 * 工单分类 Controller
 *
 * @author ticket
 */
@RestController
@RequestMapping("/ticket/category")
public class TicketCategoryController extends BaseController {

    @Autowired
    private ITicketCategoryService ticketCategoryService;

    /**
     * 查询分类列表（平铺）
     */
    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/list")
    public AjaxResult list(TicketCategory category) {
        return success(ticketCategoryService.selectCategoryList(category));
    }

    /**
     * 查询分类树
     */
    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        return success(ticketCategoryService.selectCategoryTree());
    }

    /**
     * 根据 ID 查询分类详情
     */
    @PreAuthorize("@ss.hasPermi('ticket:category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        TicketCategory category = ticketCategoryService.selectCategoryById(id);
        if (category == null) {
            return error("分类不存在");
        }
        return success(category);
    }

    /**
     * 新增分类
     */
    @Log(title = "分类管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:category:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCategory category) {
        return toAjax(ticketCategoryService.insertCategory(category));
    }

    /**
     * 修改分类
     */
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TicketCategory category) {
        return toAjax(ticketCategoryService.updateCategory(category));
    }

    /**
     * 删除分类
     */
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(ticketCategoryService.deleteCategoryById(id));
    }
}
