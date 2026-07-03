package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.TicketCustomFieldDefinition;
import com.ruoyi.ticket.domain.TicketCustomFieldOption;
import com.ruoyi.ticket.dto.TicketCustomFieldDefinitionDTO;
import com.ruoyi.ticket.dto.TicketCustomFieldOptionDTO;
import com.ruoyi.ticket.enums.TicketCustomFieldType;
import com.ruoyi.ticket.mapper.TicketCategoryMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldDefinitionMapper;
import com.ruoyi.ticket.mapper.TicketCustomFieldOptionMapper;
import com.ruoyi.ticket.service.ITicketCustomFieldDefinitionService;
import com.ruoyi.ticket.vo.TicketCustomFieldDefinitionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 自定义字段定义管理 Service 实现。 */
@Service
public class TicketCustomFieldDefinitionServiceImpl implements ITicketCustomFieldDefinitionService {
    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
    private static final String ENABLED = "0";
    private static final String DISABLED = "1";
    private static final int MAX_TEXT_LENGTH = 10_000;

    @Autowired private TicketCustomFieldDefinitionMapper definitionMapper;
    @Autowired private TicketCustomFieldOptionMapper optionMapper;
    @Autowired private TicketCategoryMapper categoryMapper;

    @Override
    public List<TicketCustomFieldDefinitionVO> selectByCategoryId(Long categoryId) {
        return definitionMapper.selectByCategoryId(categoryId).stream().map(this::toVO).toList();
    }

    @Override
    public TicketCustomFieldDefinitionVO selectById(Long fieldId) { return toVO(requireDefinition(fieldId)); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertDefinition(TicketCustomFieldDefinitionDTO dto) {
        validate(dto);
        if (categoryMapper.selectCategoryById(dto.getCategoryId()) == null) throw new ServiceException("工单分类不存在");
        if (definitionMapper.selectByCategoryAndKey(dto.getCategoryId(), dto.getFieldKey()) != null) {
            throw new ServiceException("分类下字段key已存在");
        }
        TicketCustomFieldDefinition definition = new TicketCustomFieldDefinition();
        copyDefinition(dto, definition); definition.setCreateBy(SecurityUtils.getUsername());
        definition.setCreateTime(new Date()); definitionMapper.insertDefinition(definition);
        saveOptions(definition.getFieldId(), dto.getOptions(), false);
        return definition.getFieldId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDefinition(Long fieldId, TicketCustomFieldDefinitionDTO dto) {
        TicketCustomFieldDefinition definition = requireDefinition(fieldId);
        if (!definition.getCategoryId().equals(dto.getCategoryId())
                || !definition.getFieldKey().equals(dto.getFieldKey())
                || !definition.getFieldType().equals(dto.getFieldType())) {
            throw new ServiceException("分类、字段key和字段类型不允许修改");
        }
        validate(dto); copyDefinition(dto, definition); definition.setUpdateBy(SecurityUtils.getUsername());
        definition.setUpdateTime(new Date()); int rows = definitionMapper.updateDefinition(definition);
        optionMapper.disableByFieldId(fieldId, SecurityUtils.getUsername());
        saveOptions(fieldId, dto.getOptions(), true);
        return rows;
    }

    private TicketCustomFieldDefinition requireDefinition(Long fieldId) {
        TicketCustomFieldDefinition value = definitionMapper.selectById(fieldId);
        if (value == null) throw new ServiceException("自定义字段不存在");
        return value;
    }

    private void validate(TicketCustomFieldDefinitionDTO dto) {
        if (dto.getFieldKey() == null || !FIELD_KEY_PATTERN.matcher(dto.getFieldKey()).matches()) {
            throw new ServiceException("字段key格式无效");
        }
        TicketCustomFieldType type;
        try { type = TicketCustomFieldType.valueOf(dto.getFieldType()); }
        catch (IllegalArgumentException | NullPointerException exception) { throw new ServiceException("字段类型无效"); }
        if (!ENABLED.equals(dto.getRequiredFlag()) && !"1".equals(dto.getRequiredFlag())) {
            throw new ServiceException("必填标志无效");
        }
        if (!ENABLED.equals(dto.getStatus()) && !DISABLED.equals(dto.getStatus())) throw new ServiceException("字段状态无效");
        if (type == TicketCustomFieldType.TEXT && dto.getMaxLength() != null
                && (dto.getMaxLength() <= 0 || dto.getMaxLength() > MAX_TEXT_LENGTH)) {
            throw new ServiceException("文本最大长度无效");
        }
        if (type == TicketCustomFieldType.NUMBER && dto.getMinNumber() != null && dto.getMaxNumber() != null
                && dto.getMinNumber().compareTo(dto.getMaxNumber()) > 0) throw new ServiceException("数字范围无效");
        boolean select = type == TicketCustomFieldType.SINGLE_SELECT || type == TicketCustomFieldType.MULTI_SELECT;
        List<TicketCustomFieldOptionDTO> options = dto.getOptions() == null ? List.of() : dto.getOptions();
        if (select && options.isEmpty()) throw new ServiceException("选择字段必须配置选项");
        if (!select && !options.isEmpty()) throw new ServiceException("非选择字段不能配置选项");
        Set<String> values = new HashSet<>();
        for (TicketCustomFieldOptionDTO option : options) {
            if (option.getOptionValue() == null || option.getOptionValue().isBlank()
                    || !values.add(option.getOptionValue())) throw new ServiceException("选项值为空或重复");
            if (!ENABLED.equals(option.getStatus()) && !DISABLED.equals(option.getStatus())) {
                throw new ServiceException("选项状态无效");
            }
        }
    }

    private void copyDefinition(TicketCustomFieldDefinitionDTO dto, TicketCustomFieldDefinition value) {
        value.setCategoryId(dto.getCategoryId()); value.setFieldKey(dto.getFieldKey());
        value.setFieldName(dto.getFieldName()); value.setFieldType(dto.getFieldType());
        value.setRequiredFlag(dto.getRequiredFlag()); value.setDefaultValue(dto.getDefaultValue());
        value.setMaxLength(dto.getMaxLength()); value.setMinNumber(dto.getMinNumber()); value.setMaxNumber(dto.getMaxNumber());
        value.setSortOrder(dto.getSortOrder()); value.setStatus(dto.getStatus()); value.setRemark(dto.getRemark());
    }

    private void saveOptions(Long fieldId, List<TicketCustomFieldOptionDTO> options, boolean updating) {
        if (options == null) return;
        for (TicketCustomFieldOptionDTO dto : options) {
            TicketCustomFieldOption option = new TicketCustomFieldOption(); BeanUtils.copyProperties(dto, option);
            option.setFieldId(fieldId);
            if (updating && dto.getOptionId() != null) {
                TicketCustomFieldOption existing = optionMapper.selectById(dto.getOptionId());
                if (existing == null || !fieldId.equals(existing.getFieldId())
                        || !existing.getOptionValue().equals(dto.getOptionValue())) {
                    throw new ServiceException("选项不存在或选项值不允许修改");
                }
                option.setUpdateBy(SecurityUtils.getUsername()); option.setUpdateTime(new Date());
                optionMapper.updateOption(option);
            } else {
                option.setCreateBy(SecurityUtils.getUsername()); option.setCreateTime(new Date());
                optionMapper.insertOption(option);
            }
        }
    }

    private TicketCustomFieldDefinitionVO toVO(TicketCustomFieldDefinition value) {
        TicketCustomFieldDefinitionVO vo = new TicketCustomFieldDefinitionVO(); BeanUtils.copyProperties(value, vo);
        vo.setOptions(optionMapper.selectByFieldId(value.getFieldId())); return vo;
    }
}
