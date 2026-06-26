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
import com.ruoyi.ticket.dto.TicketCategoryCreateDTO;
import com.ruoyi.ticket.dto.TicketCategoryQueryDTO;
import com.ruoyi.ticket.dto.TicketCategoryUpdateDTO;
import com.ruoyi.ticket.service.ITicketCategoryService;
import com.ruoyi.ticket.vo.TicketApiResponseVO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 工单分类 Controller
 *
 * @author ticket
 */
@Tag(name = "工单分类管理", description = "工单分类的增删改查及树形结构查询")
@RestController
@RequestMapping("/ticket/category")
public class TicketCategoryController extends BaseController {

    @Autowired
    private ITicketCategoryService ticketCategoryService;

    @Operation(summary = "查询分类列表（平铺）")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketCategoryListResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/list")
    public AjaxResult list(TicketCategoryQueryDTO query) {
        return success(ticketCategoryService.selectCategoryList(query));
    }

    @Operation(summary = "查询分类树")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketCategoryTreeResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:category:list')")
    @GetMapping("/tree")
    public AjaxResult tree() {
        return success(ticketCategoryService.selectCategoryTree());
    }

    @Operation(summary = "根据ID查询分类详情")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.TicketCategoryResult.class)))
    @PreAuthorize("@ss.hasPermi('ticket:category:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(ticketCategoryService.selectCategoryById(id));
    }

    @Operation(summary = "新增分类")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "分类管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:category:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCategoryCreateDTO dto) {
        return toAjax(ticketCategoryService.insertCategory(dto));
    }

    @Operation(summary = "修改分类")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:edit')")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TicketCategoryUpdateDTO dto) {
        return toAjax(ticketCategoryService.updateCategory(dto));
    }

    @Operation(summary = "删除分类")
    @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(schema = @Schema(implementation = TicketApiResponseVO.OperationResult.class)))
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ticket:category:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(ticketCategoryService.deleteCategoryById(id));
    }
}
