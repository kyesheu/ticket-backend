package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.dto.TicketAiAssistRequestDTO;
import com.ruoyi.ticket.dto.TicketAiContextDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.dto.TicketAiSimilarSearchDTO;
import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
import com.ruoyi.ticket.vo.TicketAiClosedTicketSyncVO;
import com.ruoyi.ticket.vo.TicketAiDocumentDetailVO;
import com.ruoyi.ticket.vo.TicketAiDocumentListVO;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;
import com.ruoyi.ticket.vo.TicketAiSimilarSearchResultVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;

/**
 * 工单 AI HTTP interface。
 */
public interface ITicketAiService {

    /** 查询 Python AI 服务健康状态。 */
    TicketAiHealthVO health();

    /** 导入知识文档。 */
    TicketAiAcceptedVO importDocument(TicketAiDocumentImportDTO dto);

    /** 分页查询知识文档。 */
    TicketAiDocumentListVO listDocuments(TicketAiDocumentQueryDTO dto);

    /** 查询知识文档详情。 */
    TicketAiDocumentDetailVO getDocument(String sourceId);

    /** 同步已关闭工单知识快照。 */
    TicketAiClosedTicketSyncVO syncClosedTicket(TicketAiClosedTicketSyncDTO dto);

    /** 检索相似知识。 */
    TicketAiSearchResultVO search(TicketAiContextDTO dto);

    /** 检索相似历史工单。 */
    TicketAiSimilarSearchResultVO searchSimilarTickets(TicketAiSimilarSearchDTO dto);

    /** 生成处理建议与回复草稿。 */
    TicketAiAssistVO assist(TicketAiAssistRequestDTO dto);

    /** 生成受控分诊建议。 */
    TicketAiTriageVO triage(TicketAiTriageRequestDTO dto);
}
