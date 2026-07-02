package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketSlaPolicy;
import com.ruoyi.ticket.dto.TicketSlaPolicyDTO;
import com.ruoyi.ticket.mapper.TicketSlaPolicyMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单 SLA 策略 Service 单元测试
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单 SLA 策略 Service 测试")
class TicketSlaPolicyServiceImplTest {

    @Mock
    private TicketSlaPolicyMapper ticketSlaPolicyMapper;

    @InjectMocks
    private TicketSlaPolicyServiceImpl ticketSlaPolicyService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("新增唯一优先级策略应成功")
    void insertUniquePolicyShouldSucceed() {
        TicketSlaPolicyDTO dto = createValidDTO();
        when(ticketSlaPolicyMapper.selectPolicyByPriority("HIGH")).thenReturn(null);
        when(ticketSlaPolicyMapper.insertPolicy(any(TicketSlaPolicy.class))).thenReturn(1);

        ticketSlaPolicyService.insertPolicy(dto);

        verify(ticketSlaPolicyMapper).insertPolicy(any(TicketSlaPolicy.class));
    }

    @Test
    @DisplayName("新增重复优先级策略应拒绝")
    void insertDuplicatePriorityShouldThrow() {
        TicketSlaPolicyDTO dto = createValidDTO();
        when(ticketSlaPolicyMapper.selectPolicyByPriority("HIGH")).thenReturn(createPolicy(1L));

        assertThatThrownBy(() -> ticketSlaPolicyService.insertPolicy(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已存在");
        verify(ticketSlaPolicyMapper, never()).insertPolicy(any());
    }

    @Test
    @DisplayName("解决时限不大于响应时限应拒绝")
    void invalidMinuteRelationShouldThrow() {
        TicketSlaPolicyDTO dto = createValidDTO();
        dto.setResolveMinutes(60);

        assertThatThrownBy(() -> ticketSlaPolicyService.insertPolicy(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("解决时限必须大于首次响应时限");
        verify(ticketSlaPolicyMapper, never()).insertPolicy(any());
    }

    @Test
    @DisplayName("非法优先级应拒绝")
    void invalidPriorityShouldThrow() {
        TicketSlaPolicyDTO dto = createValidDTO();
        dto.setPriority("UNKNOWN");

        assertThatThrownBy(() -> ticketSlaPolicyService.insertPolicy(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("优先级无效");
    }

    @Test
    @DisplayName("修改策略时允许保留自身优先级")
    void updateWithSamePriorityShouldSucceed() {
        TicketSlaPolicy existing = createPolicy(1L);
        TicketSlaPolicyDTO dto = createValidDTO();
        when(ticketSlaPolicyMapper.selectPolicyById(1L)).thenReturn(existing);
        when(ticketSlaPolicyMapper.selectPolicyByPriority("HIGH")).thenReturn(existing);
        when(ticketSlaPolicyMapper.updatePolicy(any(TicketSlaPolicy.class))).thenReturn(1);

        ticketSlaPolicyService.updatePolicy(1L, dto);

        verify(ticketSlaPolicyMapper).updatePolicy(any(TicketSlaPolicy.class));
    }

    @Test
    @DisplayName("修改不存在策略应拒绝")
    void updateMissingPolicyShouldThrow() {
        when(ticketSlaPolicyMapper.selectPolicyById(99L)).thenReturn(null);

        assertThatThrownBy(() -> ticketSlaPolicyService.updatePolicy(99L, createValidDTO()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("策略不存在");
    }

    @Test
    @DisplayName("查询不存在策略应拒绝")
    void selectMissingPolicyShouldThrow() {
        when(ticketSlaPolicyMapper.selectPolicyById(99L)).thenReturn(null);

        assertThatThrownBy(() -> ticketSlaPolicyService.selectPolicyById(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("策略不存在");
    }

    private TicketSlaPolicyDTO createValidDTO() {
        TicketSlaPolicyDTO dto = new TicketSlaPolicyDTO();
        dto.setPriority("HIGH");
        dto.setResponseMinutes(60);
        dto.setResolveMinutes(480);
        dto.setStatus("0");
        dto.setRemark("测试策略");
        return dto;
    }

    private TicketSlaPolicy createPolicy(Long policyId) {
        TicketSlaPolicy policy = new TicketSlaPolicy();
        policy.setPolicyId(policyId);
        policy.setPriority("HIGH");
        return policy;
    }
}
