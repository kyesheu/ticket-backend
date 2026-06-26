package com.ruoyi.ticket.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.dto.TicketCommentDTO;
import com.ruoyi.ticket.service.ITicketCommentService;

/**
 * 工单评论 Controller
 *
 * @author ticket
 */
@RestController
@RequestMapping("/ticket/{ticketId}/comment")
public class TicketCommentController extends BaseController {

    @Autowired
    private ITicketCommentService ticketCommentService;

    /**
     * 查看工单评论列表
     */
    @PreAuthorize("@ss.hasPermi('ticket:comment:list')")
    @GetMapping
    public AjaxResult list(@PathVariable Long ticketId) {
        List<TicketComment> comments = ticketCommentService.selectCommentsByTicketId(ticketId);
        return success(comments);
    }

    /**
     * 添加工单评论
     */
    @Log(title = "工单评论", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:comment:add')")
    @PostMapping
    public AjaxResult add(@PathVariable Long ticketId, @Validated @RequestBody TicketCommentDTO dto) {
        ticketCommentService.addComment(ticketId, dto);
        return success();
    }
}
