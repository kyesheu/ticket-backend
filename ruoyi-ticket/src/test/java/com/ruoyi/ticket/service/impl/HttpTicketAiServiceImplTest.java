package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ticket.config.TicketAiProperties;
import com.ruoyi.ticket.dto.TicketAiContextDTO;
import com.ruoyi.ticket.exception.TicketAiServiceException;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Python AI 服务 HTTP adapter 测试。
 */
@DisplayName("Python AI 服务 HTTP adapter 测试")
class HttpTicketAiServiceImplTest {

    private static final String SERVICE_TOKEN = "test-service-token-12345";

    private HttpServer server;
    private TicketAiProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        properties = new TicketAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setServiceToken(SERVICE_TOKEN);
        properties.setConnectTimeout(Duration.ofSeconds(1L));
        properties.setReadTimeout(Duration.ofSeconds(1L));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("健康检查解析 v1 契约")
    void shouldReadHealthContract() {
        server.createContext("/api/v1/health", exchange -> respond(exchange, 200,
                "{\"status\":\"UP\",\"contract_version\":\"v1\"}"));

        TicketAiHealthVO result = createService().health();

        assertThat(result.getStatus()).isEqualTo("UP");
        assertThat(result.getContractVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("业务请求使用服务凭据和 snake_case 契约")
    void shouldSendCredentialAndSnakeCaseContract() {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/api/v1/knowledge/search", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Service-Token"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"sources\":[]}");
        });
        TicketAiContextDTO dto = new TicketAiContextDTO();
        dto.setTicketNo("TK202607040001");
        dto.setTitle("Redis 缓存穿透");
        dto.setDescription("如何处理？");
        dto.setPriority("HIGH");

        createService().search(dto);

        assertThat(token.get()).isEqualTo(SERVICE_TOKEN);
        assertThat(body.get()).contains("\"contract_version\":\"v1\"")
                .contains("\"ticket_no\":\"TK202607040001\"");
    }

    @Test
    @DisplayName("非法 JSON 响应转换为统一异常")
    void shouldRejectInvalidJson() {
        server.createContext("/api/v1/health", exchange -> respond(exchange, 200, "not-json"));

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务调用失败");
    }

    @Test
    @DisplayName("不兼容的健康检查契约版本被拒绝")
    void shouldRejectIncompatibleContractVersion() {
        server.createContext("/api/v1/health", exchange -> respond(exchange, 200,
                "{\"status\":\"UP\",\"contract_version\":\"v2\"}"));

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务契约版本不兼容");
    }

    @Test
    @DisplayName("超大响应在反序列化前被拒绝")
    void shouldRejectOversizedResponse() {
        properties.setMaxResponseBytes(16);
        server.createContext("/api/v1/health", exchange -> respond(exchange, 200,
                "{\"status\":\"UP\",\"contract_version\":\"v1\"}"));

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务响应超过大小限制");
    }

    @Test
    @DisplayName("非成功 HTTP 状态不向调用方透传响应正文")
    void shouldRejectErrorStatusWithoutLeakingBody() {
        server.createContext("/api/v1/health", exchange -> respond(exchange, 500, "secret upstream error"));

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务返回异常状态: 500")
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("Python 服务不可达时转换为统一异常")
    void shouldHandleUnavailableService() throws IOException {
        int unavailablePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unavailablePort = socket.getLocalPort();
        }
        properties.setBaseUrl("http://127.0.0.1:" + unavailablePort);

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务调用失败");
    }

    @Test
    @DisplayName("读取超过配置超时时间时快速失败")
    void shouldHandleReadTimeout() {
        properties.setReadTimeout(Duration.ofMillis(50L));
        server.createContext("/api/v1/health", exchange -> {
            try {
                Thread.sleep(200L);
                respond(exchange, 200, "{\"status\":\"UP\",\"contract_version\":\"v1\"}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> createService().health())
                .isInstanceOf(TicketAiServiceException.class)
                .hasMessage("AI 服务调用失败");
    }

    private HttpTicketAiServiceImpl createService() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        return new HttpTicketAiServiceImpl(client, new ObjectMapper(), properties);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
