package com.ruoyi.ticket.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.ruoyi.ticket.vo.TicketAiClosedTicketSyncVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 历史工单同步 DTO 契约测试。
 */
@DisplayName("历史工单同步 DTO 契约测试")
class TicketAiClosedTicketSyncDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("完整字段通过基础校验并序列化为 snake_case")
    void shouldValidateAndSerializeContract() throws Exception {
        TicketAiClosedTicketSyncDTO dto = validDto();
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        String json = mapper.writeValueAsString(dto);

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(json).contains("\"ticket_id\":42", "\"category\":\"中间件\"",
                "\"solution\":\"参数校验、空值缓存和布隆过滤器\"", "\"status\":\"CLOSED\"",
                "\"tags\":[\"Redis\",\"缓存\"]", "\"created_time\":", "\"closed_time\":",
                "\"source_generation\":3");
        assertThat(json).doesNotContain("ticket_no", "resolution");
    }

    @Test
    @DisplayName("缺少必填字段或状态非 CLOSED 时校验失败")
    void shouldRejectInvalidRequiredFields() {
        TicketAiClosedTicketSyncDTO dto = validDto();
        dto.setSolution("");
        dto.setStatus("PROCESSING");
        dto.setSourceGeneration(0L);

        assertThat(validator.validate(dto)).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("同步响应包含工单和来源代次")
    void shouldDeserializeSyncResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        TicketAiClosedTicketSyncVO response = mapper.readValue(
                "{\"accepted\":true,\"ticket_id\":42,\"source_generation\":3}",
                TicketAiClosedTicketSyncVO.class);

        assertThat(response.getAccepted()).isTrue();
        assertThat(response.getTicketId()).isEqualTo(42L);
        assertThat(response.getSourceGeneration()).isEqualTo(3L);
    }

    private TicketAiClosedTicketSyncDTO validDto() {
        TicketAiClosedTicketSyncDTO dto = new TicketAiClosedTicketSyncDTO();
        dto.setTicketId(42L);
        dto.setTitle("Redis 缓存穿透");
        dto.setCategory("中间件");
        dto.setDescription("不存在的 key 被反复查询");
        dto.setSolution("参数校验、空值缓存和布隆过滤器");
        dto.setStatus("CLOSED");
        dto.setTags(List.of("Redis", "缓存"));
        dto.setCreatedTime("2026-07-01T09:00:00+08:00");
        dto.setClosedTime("2026-07-01T10:00:00+08:00");
        dto.setSourceGeneration(3L);
        return dto;
    }
}
