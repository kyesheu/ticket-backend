package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketSlaAlert;
import com.ruoyi.ticket.dto.TicketSlaAlertQueryDTO;
import com.ruoyi.ticket.vo.TicketSlaAlertVO;

import java.util.List;

/**
 * 工单 SLA 告警 Mapper 接口
 *
 * @author ticket
 */
public interface TicketSlaAlertMapper {

    List<TicketSlaAlertVO> selectAlertList(TicketSlaAlertQueryDTO query);

    TicketSlaAlertVO selectAlertById(Long alertId);

    /**
     * 根据工单 ID 查询告警
     *
     * @param ticketId 工单 ID
     * @return SLA 告警列表
     */
    List<TicketSlaAlert> selectAlertsByTicketId(Long ticketId);

    /**
     * 新增 SLA 告警
     *
     * @param alert SLA 告警
     * @return 影响行数
     */
    int insertAlert(TicketSlaAlert alert);
}
