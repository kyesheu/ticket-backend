package com.ruoyi.ticket.controller;

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

    @PreAuthorize("@ss.hasPermi('ticket:comment:list')")
    @GetMapping
    public AjaxResult list(@PathVariable Long ticketId) {
        // TODO: 阶段六实现
        return success();
    }

    @Log(title = "工单评论", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:comment:add')")
    @PostMapping
    public AjaxResult add(@PathVariable Long ticketId, @Validated @RequestBody TicketCommentDTO dto) {
        // TODO: 阶段六实现
        return success();
    }
}
