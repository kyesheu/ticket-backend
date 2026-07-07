package com.ruoyi.ticket.domain;

import com.ruoyi.ticket.enums.TicketAiFeedbackTargetTypeEnum;
import com.ruoyi.ticket.enums.TicketAiFeedbackValueEnum;
import java.io.Serializable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 反馈模型测试。
 */
@DisplayName("AI 反馈模型测试")
class TicketAiFeedbackModelTest {

    @Test
    @DisplayName("反馈模型支持序列化并保存结构化字段")
    void shouldMakeFeedbackSerializable() {
        TicketAiFeedback feedback = new TicketAiFeedback();
        feedback.setTicketId(42L);
        feedback.setTargetType(TicketAiFeedbackTargetTypeEnum.ASSIST.name());
        feedback.setTargetId(7L);
        feedback.setFeedbackValue(TicketAiFeedbackValueEnum.USEFUL.name());
        feedback.setAdopted(Boolean.TRUE);
        feedback.setEvaluatorId(1L);

        assertThat(feedback).isInstanceOf(Serializable.class);
        assertThat(feedback.getTargetType()).isEqualTo("ASSIST");
        assertThat(feedback.getFeedbackValue()).isEqualTo("USEFUL");
        assertThat(feedback.getAdopted()).isTrue();
    }

    @Test
    @DisplayName("反馈枚举值完整")
    void shouldContainFeedbackEnumValues() {
        assertThat(TicketAiFeedbackTargetTypeEnum.values()).containsExactly(
                TicketAiFeedbackTargetTypeEnum.ASSIST,
                TicketAiFeedbackTargetTypeEnum.TRIAGE);
        assertThat(TicketAiFeedbackValueEnum.values()).containsExactly(
                TicketAiFeedbackValueEnum.USEFUL,
                TicketAiFeedbackValueEnum.NOT_USEFUL);
    }
}
