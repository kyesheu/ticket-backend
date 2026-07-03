package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketAttachment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单附件 Mapper。
 */
public interface TicketAttachmentMapper {
    TicketAttachment selectById(Long attachmentId);

    List<TicketAttachment> selectByTicketId(Long ticketId);

    List<TicketAttachment> selectByBusiness(@Param("businessType") String businessType,
                                            @Param("businessId") Long businessId);

    List<TicketAttachment> selectByIds(@Param("attachmentIds") List<Long> attachmentIds);

    int countByTicketId(Long ticketId);

    int insertAttachment(TicketAttachment attachment);

    int bindAttachments(@Param("ticketId") Long ticketId,
                        @Param("businessType") String businessType,
                        @Param("businessId") Long businessId,
                        @Param("uploaderId") Long uploaderId,
                        @Param("attachmentIds") List<Long> attachmentIds);

    int logicallyDelete(@Param("attachmentId") Long attachmentId,
                        @Param("deleteBy") String deleteBy);

    int markStorageDeleted(Long attachmentId);
}
