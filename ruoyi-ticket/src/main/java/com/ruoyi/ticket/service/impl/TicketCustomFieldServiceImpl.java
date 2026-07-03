package com.ruoyi.ticket.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.TicketCustomFieldDefinition;
import com.ruoyi.ticket.domain.TicketCustomFieldOption;
import com.ruoyi.ticket.domain.TicketCustomFieldValue;
import com.ruoyi.ticket.dto.TicketCustomFieldInputDTO;
import com.ruoyi.ticket.enums.TicketCustomFieldType;
import com.ruoyi.ticket.mapper.TicketCustomFieldDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldOptionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldValueMapper;
import com.ruoyi.ticket.service.ITicketCustomFieldService;
import com.ruoyi.ticket.vo.TicketCustomFieldDefinitionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 工单自定义字段值服务实现。
 */
@Service
public class TicketCustomFieldServiceImpl implements ITicketCustomFieldService {

    private static final String REQUIRED_FLAG = "1";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TicketCustomFieldDefinitionMapper definitionMapper;

    @Autowired
    private TicketCustomFieldOptionMapper optionMapper;

    @Autowired
    private TicketCustomFieldValueMapper valueMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<TicketCustomFieldDefinitionVO> selectFormDefinitions(Long categoryId) {
        return definitionMapper.selectEnabledByCategoryId(categoryId).stream().map(definition -> {
            TicketCustomFieldDefinitionVO vo = new TicketCustomFieldDefinitionVO();
            BeanUtils.copyProperties(definition, vo);
            vo.setOptions(optionMapper.selectEnabledByFieldId(definition.getFieldId()));
            return vo;
        }).toList();
    }

    @Override
    public void validateAndSave(Long ticketId, Long categoryId, List<TicketCustomFieldInputDTO> inputs) {
        List<TicketCustomFieldDefinition> definitions = definitionMapper.selectEnabledByCategoryId(categoryId);
        Map<String, Object> inputMap = buildInputMap(inputs);
        Set<String> definitionKeys = new HashSet<>();
        for (TicketCustomFieldDefinition definition : definitions) {
            definitionKeys.add(definition.getFieldKey());
        }
        for (String inputKey : inputMap.keySet()) {
            if (!definitionKeys.contains(inputKey)) {
                throw new ServiceException("自定义字段不存在或已停用: " + inputKey);
            }
        }
        for (TicketCustomFieldDefinition definition : definitions) {
            Object rawValue = inputMap.containsKey(definition.getFieldKey())
                    ? inputMap.get(definition.getFieldKey()) : definition.getDefaultValue();
            if (isEmpty(rawValue)) {
                if (REQUIRED_FLAG.equals(definition.getRequiredFlag())) {
                    throw new ServiceException("自定义字段不能为空: " + definition.getFieldName());
                }
                continue;
            }
            saveSnapshot(ticketId, definition, normalize(definition, rawValue));
        }
    }

    @Override
    public List<TicketCustomFieldValue> selectValueSnapshots(Long ticketId) {
        return valueMapper.selectByTicketId(ticketId);
    }

    private Map<String, Object> buildInputMap(List<TicketCustomFieldInputDTO> inputs) {
        Map<String, Object> inputMap = new HashMap<>();
        if (inputs == null) {
            return inputMap;
        }
        for (TicketCustomFieldInputDTO input : inputs) {
            if (input == null || StringUtils.isBlank(input.getFieldKey())) {
                throw new ServiceException("自定义字段键不能为空");
            }
            if (inputMap.containsKey(input.getFieldKey())) {
                throw new ServiceException("自定义字段重复: " + input.getFieldKey());
            }
            inputMap.put(input.getFieldKey(), input.getValue());
        }
        return inputMap;
    }

    private NormalizedField normalize(TicketCustomFieldDefinition definition, Object rawValue) {
        TicketCustomFieldType type;
        try {
            type = TicketCustomFieldType.valueOf(definition.getFieldType());
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("未知的自定义字段类型: " + definition.getFieldType());
        }
        return switch (type) {
            case TEXT -> normalizeText(definition, rawValue);
            case NUMBER -> normalizeNumber(definition, rawValue);
            case DATE -> normalizeDate(rawValue);
            case DATETIME -> normalizeDateTime(rawValue);
            case BOOLEAN -> normalizeBoolean(rawValue);
            case SINGLE_SELECT -> normalizeSingleSelect(definition, rawValue);
            case MULTI_SELECT -> normalizeMultiSelect(definition, rawValue);
        };
    }

    private NormalizedField normalizeText(TicketCustomFieldDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value)) {
            throw invalidValue(definition);
        }
        if (definition.getMaxLength() != null && value.length() > definition.getMaxLength()) {
            throw new ServiceException("自定义字段长度超限: " + definition.getFieldName());
        }
        return new NormalizedField(value, value);
    }

    private NormalizedField normalizeNumber(TicketCustomFieldDefinition definition, Object rawValue) {
        try {
            BigDecimal value = new BigDecimal(String.valueOf(rawValue));
            if (definition.getMinNumber() != null && value.compareTo(definition.getMinNumber()) < 0
                    || definition.getMaxNumber() != null && value.compareTo(definition.getMaxNumber()) > 0) {
                throw new ServiceException("自定义字段数值超出范围: " + definition.getFieldName());
            }
            String normalized = value.stripTrailingZeros().toPlainString();
            return new NormalizedField(normalized, normalized);
        } catch (NumberFormatException exception) {
            throw invalidValue(definition);
        }
    }

    private NormalizedField normalizeDate(Object rawValue) {
        try {
            String value = LocalDate.parse(String.valueOf(rawValue)).toString();
            return new NormalizedField(value, value);
        } catch (DateTimeParseException exception) {
            throw new ServiceException("自定义字段日期格式无效");
        }
    }

    private NormalizedField normalizeDateTime(Object rawValue) {
        try {
            String source = String.valueOf(rawValue);
            LocalDateTime value = source.contains("T") ? LocalDateTime.parse(source)
                    : LocalDateTime.parse(source, DATETIME_FORMATTER);
            String normalized = value.format(DATETIME_FORMATTER);
            return new NormalizedField(normalized, normalized);
        } catch (DateTimeParseException exception) {
            throw new ServiceException("自定义字段日期时间格式无效");
        }
    }

    private NormalizedField normalizeBoolean(Object rawValue) {
        if (rawValue instanceof Boolean value) {
            return new NormalizedField(value.toString(), value ? "是" : "否");
        }
        String value = String.valueOf(rawValue);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new ServiceException("自定义字段布尔值无效");
        }
        boolean normalized = Boolean.parseBoolean(value);
        return new NormalizedField(Boolean.toString(normalized), normalized ? "是" : "否");
    }

    private NormalizedField normalizeSingleSelect(TicketCustomFieldDefinition definition, Object rawValue) {
        String value = String.valueOf(rawValue);
        TicketCustomFieldOption option = enabledOptionMap(definition).get(value);
        if (option == null) {
            throw invalidValue(definition);
        }
        return new NormalizedField(value, option.getOptionLabel());
    }

    private NormalizedField normalizeMultiSelect(TicketCustomFieldDefinition definition, Object rawValue) {
        Collection<?> values;
        if (rawValue instanceof Collection<?> collection) {
            values = collection;
        } else if (rawValue instanceof String stringValue) {
            try {
                values = objectMapper.readValue(stringValue, List.class);
            } catch (JsonProcessingException exception) {
                throw invalidValue(definition);
            }
        } else {
            throw invalidValue(definition);
        }
        Map<String, TicketCustomFieldOption> optionMap = enabledOptionMap(definition);
        List<String> normalizedValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Set<String> uniqueValues = new HashSet<>();
        for (Object item : values) {
            String value = String.valueOf(item);
            TicketCustomFieldOption option = optionMap.get(value);
            if (option == null || !uniqueValues.add(value)) {
                throw invalidValue(definition);
            }
            normalizedValues.add(value);
            labels.add(option.getOptionLabel());
        }
        if (normalizedValues.isEmpty()) {
            throw invalidValue(definition);
        }
        try {
            return new NormalizedField(objectMapper.writeValueAsString(normalizedValues),
                    objectMapper.writeValueAsString(labels));
        } catch (JsonProcessingException exception) {
            throw new ServiceException("自定义字段序列化失败");
        }
    }

    private Map<String, TicketCustomFieldOption> enabledOptionMap(TicketCustomFieldDefinition definition) {
        Map<String, TicketCustomFieldOption> options = new HashMap<>();
        for (TicketCustomFieldOption option : optionMapper.selectEnabledByFieldId(definition.getFieldId())) {
            options.put(option.getOptionValue(), option);
        }
        return options;
    }

    private void saveSnapshot(Long ticketId, TicketCustomFieldDefinition definition, NormalizedField field) {
        TicketCustomFieldValue value = new TicketCustomFieldValue();
        value.setTicketId(ticketId);
        value.setFieldId(definition.getFieldId());
        value.setFieldKeySnapshot(definition.getFieldKey());
        value.setFieldNameSnapshot(definition.getFieldName());
        value.setFieldTypeSnapshot(definition.getFieldType());
        value.setNormalizedValue(field.normalizedValue());
        value.setDisplayValueSnapshot(field.displayValue());
        value.setSortOrderSnapshot(definition.getSortOrder());
        value.setCreateTime(new Date());
        valueMapper.insertValue(value);
    }

    private boolean isEmpty(Object value) {
        return value == null || value instanceof String stringValue && StringUtils.isBlank(stringValue)
                || value instanceof Collection<?> collection && collection.isEmpty();
    }

    private ServiceException invalidValue(TicketCustomFieldDefinition definition) {
        return new ServiceException("自定义字段值无效: " + definition.getFieldName());
    }

    private record NormalizedField(String normalizedValue, String displayValue) { }
}
