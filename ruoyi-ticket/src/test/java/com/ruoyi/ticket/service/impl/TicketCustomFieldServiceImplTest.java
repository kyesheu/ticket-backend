package com.ruoyi.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.domain.TicketCustomFieldDefinition;
import com.ruoyi.ticket.domain.TicketCustomFieldOption;
import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import com.ruoyi.ticket.dto.TicketCustomFieldInputDTO;
import com.ruoyi.ticket.mapper.TicketCustomFieldDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldOptionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldValueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 工单自定义字段值服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TicketCustomFieldServiceImplTest {

    @Mock private TicketCustomFieldDefinitionMapper definitionMapper;
    @Mock private TicketCustomFieldOptionMapper optionMapper;
    @Mock private TicketCustomFieldValueMapper valueMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private TicketCustomFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of());
    }

    @Test
    void shouldNormalizeAndSaveAllTypes() {
        List<TicketCustomFieldDefinition> definitions = List.of(
                definition(1L, "TEXT_FIELD", "TEXT"), definition(2L, "NUMBER_FIELD", "NUMBER"),
                definition(3L, "DATE_FIELD", "DATE"), definition(4L, "DATETIME_FIELD", "DATETIME"),
                definition(5L, "BOOLEAN_FIELD", "BOOLEAN"), definition(6L, "SINGLE_FIELD", "SINGLE_SELECT"),
                definition(7L, "MULTI_FIELD", "MULTI_SELECT"));
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(definitions);
        when(optionMapper.selectEnabledByFieldId(6L)).thenReturn(List.of(option("A", "甲")));
        when(optionMapper.selectEnabledByFieldId(7L)).thenReturn(List.of(option("A", "甲"), option("B", "乙")));

        service.validateAndSave(9L, 6L, List.of(input("TEXT_FIELD", "文本"), input("NUMBER_FIELD", "10.00"),
                input("DATE_FIELD", "2026-07-03"), input("DATETIME_FIELD", "2026-07-03T10:20:30"),
                input("BOOLEAN_FIELD", true), input("SINGLE_FIELD", "A"),
                input("MULTI_FIELD", List.of("A", "B"))));

        ArgumentCaptor<TicketCustomFieldValue> captor = ArgumentCaptor.forClass(TicketCustomFieldValue.class);
        verify(valueMapper, org.mockito.Mockito.times(7)).insertValue(captor.capture());
        assertThat(captor.getAllValues()).extracting(TicketCustomFieldValue::getNormalizedValue)
                .containsExactly("文本", "10", "2026-07-03", "2026-07-03 10:20:30", "true", "A", "[\"A\",\"B\"]");
        assertThat(captor.getAllValues().get(5).getDisplayValueSnapshot()).isEqualTo("甲");
    }

    @Test
    void shouldUseDefaultAndKeepDefinitionSnapshot() {
        TicketCustomFieldDefinition definition = definition(1L, "LOCATION", "TEXT");
        definition.setFieldName("创建时名称"); definition.setDefaultValue("默认值");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(definition));
        service.validateAndSave(9L, 6L, null);
        ArgumentCaptor<TicketCustomFieldValue> captor = ArgumentCaptor.forClass(TicketCustomFieldValue.class);
        verify(valueMapper).insertValue(captor.capture());
        assertThat(captor.getValue().getFieldNameSnapshot()).isEqualTo("创建时名称");
        assertThat(captor.getValue().getNormalizedValue()).isEqualTo("默认值");
    }

    @Test
    void shouldRejectRequiredMissingUnknownAndDuplicateInputs() {
        TicketCustomFieldDefinition definition = definition(1L, "LOCATION", "TEXT");
        definition.setRequiredFlag("1");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(definition));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, null))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不能为空");
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("UNKNOWN", "x"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不存在或已停用");
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L,
                List.of(input("LOCATION", "x"), input("LOCATION", "y"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("重复");
    }

    @Test
    void shouldRejectLengthNumberFormatAndBounds() {
        TicketCustomFieldDefinition text = definition(1L, "TEXT_FIELD", "TEXT"); text.setMaxLength(2);
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(text));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("TEXT_FIELD", "abc"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("长度超限");

        TicketCustomFieldDefinition number = definition(2L, "NUMBER_FIELD", "NUMBER");
        number.setMinNumber(BigDecimal.ONE); number.setMaxNumber(BigDecimal.TEN);
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(number));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("NUMBER_FIELD", "11"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("超出范围");
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("NUMBER_FIELD", "x"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("值无效");
    }

    @Test
    void shouldRejectInvalidFormatsStoppedOptionAndDuplicateMultiSelect() {
        TicketCustomFieldDefinition date = definition(1L, "DATE_FIELD", "DATE");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(date));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("DATE_FIELD", "2026-02-30"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("日期格式");

        TicketCustomFieldDefinition bool = definition(2L, "BOOL_FIELD", "BOOLEAN");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(bool));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("BOOL_FIELD", "yes"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("布尔值");

        TicketCustomFieldDefinition select = definition(3L, "SELECT_FIELD", "SINGLE_SELECT");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(select));
        when(optionMapper.selectEnabledByFieldId(3L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L, List.of(input("SELECT_FIELD", "STOPPED"))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("值无效");

        TicketCustomFieldDefinition multi = definition(4L, "MULTI_FIELD", "MULTI_SELECT");
        when(definitionMapper.selectEnabledByCategoryId(6L)).thenReturn(List.of(multi));
        when(optionMapper.selectEnabledByFieldId(4L)).thenReturn(List.of(option("A", "甲")));
        assertThatThrownBy(() -> service.validateAndSave(9L, 6L,
                List.of(input("MULTI_FIELD", List.of("A", "A")))))
                .isInstanceOf(ServiceException.class).hasMessageContaining("值无效");
        verify(valueMapper, never()).insertValue(any());
    }

    private TicketCustomFieldDefinition definition(Long id, String key, String type) {
        TicketCustomFieldDefinition definition = new TicketCustomFieldDefinition();
        definition.setFieldId(id); definition.setCategoryId(6L); definition.setFieldKey(key);
        definition.setFieldName(key); definition.setFieldType(type); definition.setRequiredFlag("0");
        definition.setSortOrder(id.intValue()); return definition;
    }

    private TicketCustomFieldOption option(String value, String label) {
        TicketCustomFieldOption option = new TicketCustomFieldOption();
        option.setOptionValue(value); option.setOptionLabel(label); return option;
    }

    private TicketCustomFieldInputDTO input(String key, Object value) {
        TicketCustomFieldInputDTO input = new TicketCustomFieldInputDTO();
        input.setFieldKey(key); input.setValue(value); return input;
    }
}
