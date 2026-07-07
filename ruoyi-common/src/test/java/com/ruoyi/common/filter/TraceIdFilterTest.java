package com.ruoyi.common.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * traceId 过滤器测试。
 */
@DisplayName("traceId 过滤器测试")
class TraceIdFilterTest {

    @Test
    @DisplayName("请求带 traceId 时写入 MDC 和响应头")
    void shouldReuseIncomingTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-123");

        new TraceIdFilter().doFilter(request, response, assertTraceId("trace-123"));

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-123");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("请求缺少 traceId 时生成新值")
    void shouldGenerateTraceIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceIdFilter().doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotBlank());

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
    }

    private FilterChain assertTraceId(String expected) {
        return (request, response) -> assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo(expected);
    }
}
