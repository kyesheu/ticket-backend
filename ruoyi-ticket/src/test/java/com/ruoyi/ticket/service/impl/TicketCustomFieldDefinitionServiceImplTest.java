package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketCustomFieldDefinition;
import com.ruoyi.ticket.dto.TicketCustomFieldDefinitionDTO;
import com.ruoyi.ticket.dto.TicketCustomFieldOptionDTO;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldOptionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 自定义字段定义管理测试。 */
@ExtendWith(MockitoExtension.class)
class TicketCustomFieldDefinitionServiceImplTest {
    @Mock private TicketCustomFieldDefinitionMapper definitionMapper;
    @Mock private TicketCustomFieldOptionMapper optionMapper;
    @Mock private TicketCategoryMapper categoryMapper;
    @InjectMocks private TicketCustomFieldDefinitionServiceImpl service;
    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() { securityMock.close(); }

    @Test
    void shouldInsertValidTextField() {
        TicketCustomFieldDefinitionDTO dto = textDTO();
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        when(definitionMapper.selectByCategoryAndKey(6L, "LOCATION")).thenReturn(null);
        when(definitionMapper.insertDefinition(any())).thenAnswer(invocation -> {
            invocation.<TicketCustomFieldDefinition>getArgument(0).setFieldId(10L); return 1;
        });
        service.insertDefinition(dto);
        verify(definitionMapper).insertDefinition(any());
        verify(optionMapper, never()).insertOption(any());
    }

    @Test
    void shouldRejectDuplicateFieldKey() {
        when(categoryMapper.selectCategoryById(6L)).thenReturn(new TicketCategory());
        when(definitionMapper.selectByCategoryAndKey(6L, "LOCATION"))
                .thenReturn(definition("TEXT"));
        assertThatThrownBy(() -> service.insertDefinition(textDTO()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已存在");
    }

    @Test
    void shouldRejectImmutablePropertyChanges() {
        TicketCustomFieldDefinition existing = definition("TEXT"); existing.setFieldId(10L);
        when(definitionMapper.selectById(10L)).thenReturn(existing);
        TicketCustomFieldDefinitionDTO dto = textDTO(); dto.setFieldType("NUMBER");
        assertThatThrownBy(() -> service.updateDefinition(10L, dto))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不允许修改");
    }

    @Test
    void shouldRejectInvalidNumberRange() {
        TicketCustomFieldDefinitionDTO dto = textDTO(); dto.setFieldType("NUMBER");
        dto.setMaxLength(null); dto.setMinNumber(BigDecimal.TEN); dto.setMaxNumber(BigDecimal.ONE);
        assertThatThrownBy(() -> service.insertDefinition(dto))
                .isInstanceOf(ServiceException.class).hasMessageContaining("数字范围");
    }

    @Test
    void shouldRejectDuplicateSelectOptions() {
        TicketCustomFieldDefinitionDTO dto = textDTO(); dto.setFieldType("SINGLE_SELECT"); dto.setMaxLength(null);
        dto.setOptions(List.of(option("A"), option("A")));
        assertThatThrownBy(() -> service.insertDefinition(dto))
                .isInstanceOf(ServiceException.class).hasMessageContaining("重复");
    }

    @Test
    void shouldDisableOldOptionsBeforeUpdatingSelectionField() {
        TicketCustomFieldDefinition existing = definition("SINGLE_SELECT"); existing.setFieldId(10L);
        when(definitionMapper.selectById(10L)).thenReturn(existing);
        when(definitionMapper.updateDefinition(any())).thenReturn(1);
        TicketCustomFieldDefinitionDTO dto = textDTO(); dto.setFieldType("SINGLE_SELECT"); dto.setMaxLength(null);
        dto.setOptions(List.of(option("A")));
        service.updateDefinition(10L, dto);
        verify(optionMapper).disableByFieldId(10L, "admin");
        verify(optionMapper).insertOption(any());
    }

    private TicketCustomFieldDefinitionDTO textDTO() {
        TicketCustomFieldDefinitionDTO dto = new TicketCustomFieldDefinitionDTO(); dto.setCategoryId(6L);
        dto.setFieldKey("LOCATION"); dto.setFieldName("位置"); dto.setFieldType("TEXT");
        dto.setRequiredFlag("0"); dto.setStatus("0"); dto.setMaxLength(200); dto.setSortOrder(1);
        return dto;
    }

    private TicketCustomFieldDefinition definition(String type) {
        TicketCustomFieldDefinition value = new TicketCustomFieldDefinition(); value.setCategoryId(6L);
        value.setFieldKey("LOCATION"); value.setFieldType(type); return value;
    }

    private TicketCustomFieldOptionDTO option(String value) {
        TicketCustomFieldOptionDTO dto = new TicketCustomFieldOptionDTO(); dto.setOptionValue(value);
        dto.setOptionLabel(value); dto.setStatus("0"); dto.setSortOrder(1); return dto;
    }
}
