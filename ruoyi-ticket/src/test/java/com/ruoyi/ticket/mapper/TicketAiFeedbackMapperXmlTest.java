package com.ruoyi.ticket.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 反馈 Mapper XML 测试。
 */
@DisplayName("AI 反馈 Mapper XML 测试")
class TicketAiFeedbackMapperXmlTest {

    @Test
    @DisplayName("反馈 SQL 使用结构化字段且不拼接用户输入")
    void shouldUseStructuredFieldsWithoutSqlInjectionRisk() throws IOException {
        String xml;
        try (var input = getClass().getClassLoader().getResourceAsStream("mapper/TicketAiFeedbackMapper.xml")) {
            assertThat(input).isNotNull();
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(xml).contains("ticket_ai_feedback", "id=\"insertFeedback\"",
                "id=\"selectByEvaluatorAndTarget\"", "evaluator_id = #{evaluatorId}",
                "target_type = #{targetType}", "target_id = #{targetId}");
        assertThat(xml).doesNotContain("${");
    }
}
