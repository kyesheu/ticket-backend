package com.ruoyi.ticket.service;

import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
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
}
