package com.ruoyi.ticket.service;

import com.ruoyi.ticket.model.TicketAttachmentDownload;
import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
import com.ruoyi.ticket.vo.TicketAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 工单附件 Service 接口。
 */
public interface ITicketAttachmentService {
    TicketAttachmentVO uploadTemporary(MultipartFile file) throws IOException;

    List<TicketAttachmentVO> selectByTicketId(Long ticketId);

    TicketAttachmentDownload loadForDownload(Long attachmentId);

    void bindAttachments(Long ticketId, TicketAttachmentBusinessType businessType,
                         Long businessId, List<Long> attachmentIds);

    void deleteAttachment(Long attachmentId);
}
