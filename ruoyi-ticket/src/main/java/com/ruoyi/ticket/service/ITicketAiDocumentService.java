package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import com.ruoyi.ticket.vo.TicketAiDocumentDetailVO;
import com.ruoyi.ticket.vo.TicketAiDocumentListVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文档导入 Service 接口。
 */
public interface ITicketAiDocumentService {

    /**
     * 校验并转发知识文档。
     *
     * @param sourceId 稳定来源标识
     * @param file 上传文件
     * @return Python 导入结果
     */
    TicketAiAcceptedVO importDocument(String sourceId, MultipartFile file);

    /**
     * 分页查询知识文档。
     *
     * @param query 查询条件
     * @return 文档分页结果
     */
    TicketAiDocumentListVO listDocuments(TicketAiDocumentQueryDTO query);

    /**
     * 查询知识文档详情。
     *
     * @param sourceId 稳定来源标识
     * @return 文档详情
     */
    TicketAiDocumentDetailVO getDocument(String sourceId);

    /** 删除知识文档。 */
    TicketAiAcceptedVO deleteDocument(String sourceId);

    /** 重导知识文档。 */
    TicketAiAcceptedVO reimportDocument(String sourceId);
}
