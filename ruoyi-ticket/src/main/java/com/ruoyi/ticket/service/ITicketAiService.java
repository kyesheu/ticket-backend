package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.dto.TicketAiContextDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
import com.ruoyi.ticket.vo.TicketAiClosedTicketSyncVO;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;

/**
 * 工单 AI HTTP interface。
 */
public interface ITicketAiService {

    /** 查询 Python AI 服务健康状态。 */
    TicketAiHealthVO health();

    /** 导入知识文档。 */
    TicketAiAcceptedVO importDocument(TicketAiDocumentImportDTO dto);

    /** 同步已关闭工单知识快照。 */
    TicketAiClosedTicketSyncVO syncClosedTicket(TicketAiClosedTicketSyncDTO dto);

    /** 检索相似知识。 */
    TicketAiSearchResultVO search(TicketAiContextDTO dto);

    /** 生成处理建议与回复草稿。 */
    TicketAiAssistVO assist(TicketAiContextDTO dto);
}
