package com.ruoyi.ticket.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.ticket.model.TicketAttachmentDownload;
import com.ruoyi.ticket.service.ITicketAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 工单附件 Controller。
 */
@Tag(name = "工单附件", description = "工单附件的上传、查询和下载")
@RestController
@RequestMapping("/ticket/attachment")
public class TicketAttachmentController extends BaseController {

    @Autowired
    private ITicketAttachmentService ticketAttachmentService;

    @Operation(summary = "上传临时附件")
    @Log(title = "工单附件", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:attachment:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        return success(ticketAttachmentService.uploadTemporary(file));
    }

    @Operation(summary = "查询工单附件")
    @PreAuthorize("@ss.hasPermi('ticket:attachment:list')")
    @GetMapping("/ticket/{ticketId}")
    public AjaxResult list(@PathVariable Long ticketId) {
        return success(ticketAttachmentService.selectByTicketId(ticketId));
    }

    @Operation(summary = "下载工单附件")
    @PreAuthorize("@ss.hasPermi('ticket:attachment:download')")
    @GetMapping("/{attachmentId}/download")
    public void download(@PathVariable Long attachmentId, HttpServletResponse response) throws IOException {
        TicketAttachmentDownload download = ticketAttachmentService.loadForDownload(attachmentId);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        FileUtils.setAttachmentResponseHeader(response, download.getOriginalName());
        try (InputStream inputStream = download.getResource().getInputStream()) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @Operation(summary = "删除工单附件")
    @Log(title = "工单附件", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('ticket:attachment:remove')")
    @DeleteMapping("/{attachmentId}")
    public AjaxResult delete(@PathVariable Long attachmentId) {
        ticketAttachmentService.deleteAttachment(attachmentId);
        return success();
    }
}
