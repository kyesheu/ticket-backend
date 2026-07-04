package com.ruoyi.ticket.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 历史工单同步候选 Mapper XML 测试。
 */
@DisplayName("历史工单同步候选 Mapper XML 测试")
class TicketAiSyncCandidateMapperXmlTest {

    @Test
    @DisplayName("SQL 只查询已关闭且有非空 PROCESS 日志的工单")
    void shouldRestrictSyncCandidatesInSql() throws IOException {
        String xml;
        try (var input = getClass().getClassLoader().getResourceAsStream("mapper/TicketMapper.xml")) {
            assertThat(input).isNotNull();
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(xml).contains("id=\"selectAiSyncCandidatesAfter\"", "t.status = 'CLOSED'",
                "candidate_log.operation_type = 'PROCESS'", "TRIM(candidate_log.comment) != ''",
                "t.closed_at IS NOT NULL", "t.ticket_id &gt; #{lastTicketId}", "LIMIT #{limit}");
        assertThat(xml).doesNotContain("${");
    }
}
