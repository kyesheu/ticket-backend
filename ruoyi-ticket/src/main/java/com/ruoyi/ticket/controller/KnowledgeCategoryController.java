package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
import com.ruoyi.ticket.service.IKnowledgeCategoryService;
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

@RestController
@RequestMapping("/ticket/knowledge/category")
public class KnowledgeCategoryController extends BaseController {

    @Autowired
    private IKnowledgeCategoryService knowledgeCategoryService;

    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:list')")
    @GetMapping("/list")
    public AjaxResult list(TicketCategoryQueryDTO query) {
        return success(knowledgeCategoryService.selectCategoryList(query));
    }

    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:list')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        return success(knowledgeCategoryService.selectCategoryTree());
    }

    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(knowledgeCategoryService.selectCategoryById(id));
    }

    @Log(title = "知识库分类", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCategoryCreateDTO dto) {
        return toAjax(knowledgeCategoryService.insertCategory(dto));
    }

    @Log(title = "知识库分类", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TicketCategoryUpdateDTO dto) {
        return toAjax(knowledgeCategoryService.updateCategory(dto));
    }

    @Log(title = "知识库分类", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('ticket:knowledge-category:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(knowledgeCategoryService.deleteCategoryById(id));
    }
}
