package com.ruoyi.ticket.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 反馈 SQL 脚本测试。
 */
@DisplayName("AI 反馈 SQL 脚本测试")
class TicketAiFeedbackSqlTest {

    @Test
    @DisplayName("v3.2 SQL 定义反馈唯一键和必要索引")
    void shouldDefineFeedbackTableConstraints() throws IOException {
        String sql = Files.readString(Path.of("..", "sql", "ticket-v3.2.sql"), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE ticket_ai_feedback",
                "feedback_id", "ticket_id", "target_type", "target_id",
                "feedback_value", "adopted", "evaluator_id",
                "UNIQUE KEY uk_ai_feedback_evaluator_target (evaluator_id, target_type, target_id)",
                "KEY idx_ai_feedback_ticket (ticket_id)",
                "KEY idx_ai_feedback_target_time (target_type, create_time)");
        assertThat(sql).doesNotContain("model_output", "prompt", "ticket_content");
    }
}
