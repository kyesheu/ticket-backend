package com.ruoyi.ticket.config;

import com.ruoyi.ticket.exception.TicketAiServiceException;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单 AI 健康明细测试。
 */
@DisplayName("工单 AI 健康明细测试")
class TicketAiHealthIndicatorTest {

    @Test
    @DisplayName("默认关闭时报告可降级依赖未启用")
    void shouldReportDisabledWhenAiIsOff() {
        TicketAiProperties properties = new TicketAiProperties();

        Health health = new TicketAiHealthIndicator(properties, provider(null)).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("status", "DISABLED")
                .containsEntry("required", false);
    }

    @Test
    @DisplayName("AI 服务异常时报告降级但不拉低整体 readiness")
    void shouldReportDegradedWhenAiFails() {
        TicketAiProperties properties = new TicketAiProperties();
        properties.setEnabled(true);
        ITicketAiService service = new FailingTicketAiService();

        Health health = new TicketAiHealthIndicator(properties, provider(service)).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("status", "DEGRADED")
                .containsEntry("reason", "ticket_ai_unavailable")
                .containsEntry("required", false);
    }

    @Test
    @DisplayName("AI 服务正常时报告契约和依赖明细")
    void shouldReportAiDependencyDetails() {
        TicketAiProperties properties = new TicketAiProperties();
        properties.setEnabled(true);

        Health health = new TicketAiHealthIndicator(properties, provider(new HealthyTicketAiService())).health();

        assertThat(health.getDetails()).containsEntry("status", "UP")
                .containsEntry("contractVersion", "v1")
                .containsEntry("elasticsearchAvailable", true);
    }

    private ObjectProvider<ITicketAiService> provider(ITicketAiService service) {
        return new ObjectProvider<>() {
            @Override
            public ITicketAiService getObject(Object... args) {
                return service;
            }

            @Override
            public ITicketAiService getIfAvailable() {
                return service;
            }

            @Override
            public ITicketAiService getIfUnique() {
                return service;
            }

            @Override
            public ITicketAiService getObject() {
                return service;
            }
        };
    }

    private static class HealthyTicketAiService implements ITicketAiService {
        @Override
        public TicketAiHealthVO health() {
            TicketAiHealthVO value = new TicketAiHealthVO();
            value.setStatus("UP");
            value.setContractVersion("v1");
            value.setElasticsearchAvailable(true);
            value.setEmbeddingConfigured(true);
            value.setLlmConfigured(true);
            return value;
        }

        @Override
        public com.ruoyi.ticket.vo.TicketAiAcceptedVO importDocument(
                com.ruoyi.ticket.dto.TicketAiDocumentImportDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiDocumentListVO listDocuments(
                com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiDocumentDetailVO getDocument(String sourceId) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiClosedTicketSyncVO syncClosedTicket(
                com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiSearchResultVO search(
                com.ruoyi.ticket.dto.TicketAiContextDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiSimilarSearchResultVO searchSimilarTickets(
                com.ruoyi.ticket.dto.TicketAiSimilarSearchDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiAssistVO assist(
                com.ruoyi.ticket.dto.TicketAiAssistRequestDTO dto) { return null; }
        @Override
        public com.ruoyi.ticket.vo.TicketAiTriageVO triage(
                com.ruoyi.ticket.dto.TicketAiTriageRequestDTO dto) { return null; }
    }

    private static class FailingTicketAiService extends HealthyTicketAiService {
        @Override
        public TicketAiHealthVO health() {
            throw new TicketAiServiceException("unavailable");
        }
    }
}
