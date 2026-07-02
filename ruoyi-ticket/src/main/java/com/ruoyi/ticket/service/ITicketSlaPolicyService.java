package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketSlaPolicyDTO;
import com.ruoyi.ticket.vo.TicketSlaPolicyVO;

import java.util.List;

/**
 * 工单 SLA 策略 Service 接口
 *
 * @author ticket
 */
public interface ITicketSlaPolicyService {

    /** 查询全部 SLA 策略。 */
    List<TicketSlaPolicyVO> selectPolicyList();

    /** 根据 ID 查询 SLA 策略。 */
    TicketSlaPolicyVO selectPolicyById(Long policyId);

    /** 新增 SLA 策略。 */
    int insertPolicy(TicketSlaPolicyDTO dto);

    /** 修改 SLA 策略。 */
    int updatePolicy(Long policyId, TicketSlaPolicyDTO dto);
}
