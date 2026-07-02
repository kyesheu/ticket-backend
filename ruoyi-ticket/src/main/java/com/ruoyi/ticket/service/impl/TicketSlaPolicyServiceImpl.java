package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketSlaPolicy;
import com.ruoyi.ticket.dto.TicketSlaPolicyDTO;
import com.ruoyi.ticket.enums.TicketPriority;
import com.ruoyi.ticket.mapper.TicketSlaPolicyMapper;
import com.ruoyi.ticket.service.ITicketSlaPolicyService;
import com.ruoyi.ticket.vo.TicketSlaPolicyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 工单 SLA 策略 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketSlaPolicyServiceImpl implements ITicketSlaPolicyService {

    private static final String ENABLED_STATUS = "0";
    private static final String DISABLED_STATUS = "1";

    @Autowired
    private TicketSlaPolicyMapper ticketSlaPolicyMapper;

    @Override
    public List<TicketSlaPolicyVO> selectPolicyList() {
        return ticketSlaPolicyMapper.selectPolicyList().stream().map(this::toVO).toList();
    }

    @Override
    public TicketSlaPolicyVO selectPolicyById(Long policyId) {
        return toVO(requirePolicy(policyId));
    }

    @Override
    public int insertPolicy(TicketSlaPolicyDTO dto) {
        validatePolicy(dto);
        if (ticketSlaPolicyMapper.selectPolicyByPriority(dto.getPriority()) != null) {
            throw new ServiceException("该优先级的 SLA 策略已存在");
        }

        TicketSlaPolicy policy = new TicketSlaPolicy();
        copyProperties(dto, policy);
        policy.setCreateBy(SecurityUtils.getUsername());
        policy.setCreateTime(new Date());
        return ticketSlaPolicyMapper.insertPolicy(policy);
    }

    @Override
    public int updatePolicy(Long policyId, TicketSlaPolicyDTO dto) {
        TicketSlaPolicy existing = requirePolicy(policyId);
        validatePolicy(dto);
        TicketSlaPolicy samePriority = ticketSlaPolicyMapper.selectPolicyByPriority(dto.getPriority());
        if (samePriority != null && !Objects.equals(samePriority.getPolicyId(), policyId)) {
            throw new ServiceException("该优先级的 SLA 策略已存在");
        }

        copyProperties(dto, existing);
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(new Date());
        return ticketSlaPolicyMapper.updatePolicy(existing);
    }

    private TicketSlaPolicy requirePolicy(Long policyId) {
        TicketSlaPolicy policy = ticketSlaPolicyMapper.selectPolicyById(policyId);
        if (policy == null) {
            throw new ServiceException("SLA 策略不存在");
        }
        return policy;
    }

    private void validatePolicy(TicketSlaPolicyDTO dto) {
        try {
            TicketPriority.valueOf(dto.getPriority());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ServiceException("工单优先级无效");
        }
        if (dto.getResponseMinutes() == null || dto.getResponseMinutes() <= 0) {
            throw new ServiceException("首次响应时限必须大于 0");
        }
        if (dto.getResolveMinutes() == null || dto.getResolveMinutes() <= dto.getResponseMinutes()) {
            throw new ServiceException("解决时限必须大于首次响应时限");
        }
        if (!ENABLED_STATUS.equals(dto.getStatus()) && !DISABLED_STATUS.equals(dto.getStatus())) {
            throw new ServiceException("SLA 策略状态无效");
        }
    }

    private void copyProperties(TicketSlaPolicyDTO dto, TicketSlaPolicy policy) {
        policy.setPriority(dto.getPriority());
        policy.setResponseMinutes(dto.getResponseMinutes());
        policy.setResolveMinutes(dto.getResolveMinutes());
        policy.setStatus(dto.getStatus());
        policy.setRemark(dto.getRemark());
    }

    private TicketSlaPolicyVO toVO(TicketSlaPolicy policy) {
        TicketSlaPolicyVO vo = new TicketSlaPolicyVO();
        BeanUtils.copyProperties(policy, vo);
        return vo;
    }
}
