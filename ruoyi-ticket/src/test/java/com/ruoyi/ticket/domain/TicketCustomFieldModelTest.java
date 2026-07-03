package com.ruoyi.ticket.domain;

import com.ruoyi.ticket.enums.TicketCustomFieldType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/** 工单自定义字段基础模型测试。 */
@DisplayName("工单自定义字段基础模型测试")
class TicketCustomFieldModelTest {

    @Test
    void shouldSupportSevenFieldTypes() {
        assertThat(TicketCustomFieldType.values()).containsExactly(
                TicketCustomFieldType.TEXT,
                TicketCustomFieldType.NUMBER,
                TicketCustomFieldType.DATE,
                TicketCustomFieldType.DATETIME,
                TicketCustomFieldType.SINGLE_SELECT,
                TicketCustomFieldType.MULTI_SELECT,
                TicketCustomFieldType.BOOLEAN);
    }

    @Test
    void shouldMakeAllModelsSerializable() {
        assertThat(new TicketCustomFieldDefinition()).isInstanceOf(Serializable.class);
        assertThat(new TicketCustomFieldOption()).isInstanceOf(Serializable.class);
        assertThat(new TicketCustomFieldValue()).isInstanceOf(Serializable.class);
    }
}
