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

    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/list")
    public AjaxResult list(TicketCategory category) {
        // TODO: 阶段四实现
        return success();
    }

    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        // TODO: 阶段四实现
        return success();
    }

    @PreAuthorize("@ss.hasPermi('ticket:category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        // TODO: 阶段四实现
        return success();
    }

    @Log(title = "分类管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:category:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCategory category) {
        // TODO: 阶段四实现
        return success();
    }

    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TicketCategory category) {
        // TODO: 阶段四实现
        return success();
    }

    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        // TODO: 阶段四实现
        return success();
    }
}
