package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruoyi.common.filter.TraceIdFilter;
import com.ruoyi.ticket.config.TicketAiProperties;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.dto.TicketAiAssistRequestDTO;
import com.ruoyi.ticket.dto.TicketAiContextDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentImportDTO;
import com.ruoyi.ticket.dto.TicketAiDocumentQueryDTO;
import com.ruoyi.ticket.dto.TicketAiSimilarSearchDTO;
import com.ruoyi.ticket.dto.TicketAiTriageRequestDTO;
import com.ruoyi.ticket.exception.TicketAiServiceException;
import com.ruoyi.ticket.service.ITicketAiService;
import com.ruoyi.ticket.vo.TicketAiAcceptedVO;
import com.ruoyi.ticket.vo.TicketAiAssistVO;
import com.ruoyi.ticket.vo.TicketAiClosedTicketSyncVO;
import com.ruoyi.ticket.vo.TicketAiDocumentDetailVO;
import com.ruoyi.ticket.vo.TicketAiDocumentListVO;
import com.ruoyi.ticket.vo.TicketAiHealthVO;
import com.ruoyi.ticket.vo.TicketAiSearchResultVO;
import com.ruoyi.ticket.vo.TicketAiSimilarSearchResultVO;
import com.ruoyi.ticket.vo.TicketAiTriageVO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;

/**
 * 基于 JDK HTTP Client 的 Python AI 服务 adapter。
 */
public class HttpTicketAiServiceImpl implements ITicketAiService {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TicketAiProperties properties;

    public HttpTicketAiServiceImpl(HttpClient httpClient, ObjectMapper objectMapper,
                                   TicketAiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.properties = properties;
    }

    @Override
    public TicketAiHealthVO health() {
        HttpRequest request = baseRequest("/api/v1/health").GET().build();
        TicketAiHealthVO health = execute(request, TicketAiHealthVO.class);
        if (!"v1".equals(health.getContractVersion())) {
            throw new TicketAiServiceException("AI 服务契约版本不兼容");
        }
        return health;
    }

    @Override
    public TicketAiAcceptedVO importDocument(TicketAiDocumentImportDTO dto) {
        return post("/api/v1/documents/import", dto, TicketAiAcceptedVO.class);
    }

    @Override
    public TicketAiDocumentListVO listDocuments(TicketAiDocumentQueryDTO dto) {
        String path = "/api/v1/documents?page_num=" + dto.getPageNum()
                + "&page_size=" + dto.getPageSize();
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            path = path + "&status=" + encode(dto.getStatus());
        }
        if (dto.getCategoryName() != null && !dto.getCategoryName().isBlank()) {
            path = path + "&category_name=" + encode(dto.getCategoryName());
        }
        return get(path, TicketAiDocumentListVO.class);
    }

    @Override
    public TicketAiDocumentDetailVO getDocument(String sourceId) {
        return get("/api/v1/documents/" + encode(sourceId), TicketAiDocumentDetailVO.class);
    }

    @Override
    public TicketAiAcceptedVO deleteDocument(String sourceId) {
        return delete("/api/v1/documents/" + encode(sourceId), TicketAiAcceptedVO.class);
    }

    @Override
    public TicketAiAcceptedVO reimportDocument(String sourceId) {
        return post("/api/v1/documents/" + encode(sourceId) + "/reimport", new Object(), TicketAiAcceptedVO.class);
    }

    @Override
    public TicketAiClosedTicketSyncVO syncClosedTicket(TicketAiClosedTicketSyncDTO dto) {
        return post("/api/v1/tickets/sync", dto, TicketAiClosedTicketSyncVO.class);
    }

    @Override
    public TicketAiSearchResultVO search(TicketAiContextDTO dto) {
        return post("/api/v1/knowledge/search", dto, TicketAiSearchResultVO.class);
    }

    @Override
    public TicketAiSimilarSearchResultVO searchSimilarTickets(TicketAiSimilarSearchDTO dto) {
        return post("/api/v1/tickets/similar-search", dto, TicketAiSimilarSearchResultVO.class);
    }

    @Override
    public TicketAiAssistVO assist(TicketAiAssistRequestDTO dto) {
        return post("/api/v1/tickets/assist", dto, TicketAiAssistVO.class);
    }

    @Override
    public TicketAiTriageVO triage(TicketAiTriageRequestDTO dto) {
        return post("/api/v1/tickets/triage", dto, TicketAiTriageVO.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new TicketAiServiceException("AI 服务请求序列化失败", exception);
        }
        HttpRequest request = baseRequest(path)
                .header("Content-Type", JSON_CONTENT_TYPE)
                .header(SERVICE_TOKEN_HEADER, properties.getServiceToken())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        return execute(request, responseType);
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = baseRequest(path)
                .header(SERVICE_TOKEN_HEADER, properties.getServiceToken())
                .GET()
                .build();
        return execute(request, responseType);
    }

    private <T> T delete(String path, Class<T> responseType) {
        HttpRequest request = baseRequest(path)
                .header(SERVICE_TOKEN_HEADER, properties.getServiceToken())
                .DELETE()
                .build();
        return execute(request, responseType);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private HttpRequest.Builder baseRequest(String path) {
        URI uri = URI.create(properties.getBaseUrl()).resolve(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(properties.getReadTimeout())
                .header("Accept", JSON_CONTENT_TYPE);
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            builder.header(TraceIdFilter.TRACE_ID_HEADER, traceId);
        }
        return builder;
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            byte[] body = readBounded(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TicketAiServiceException("AI 服务返回异常状态: " + response.statusCode());
            }
            return objectMapper.readValue(body, responseType);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TicketAiServiceException("AI 服务调用被中断", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new TicketAiServiceException("AI 服务调用失败", exception);
        }
    }

    private byte[] readBounded(InputStream inputStream) throws IOException {
        try (inputStream) {
            byte[] body = inputStream.readNBytes(properties.getMaxResponseBytes() + 1);
            if (body.length > properties.getMaxResponseBytes()) {
                throw new TicketAiServiceException("AI 服务响应超过大小限制");
            }
            return body;
        }
    }
}
