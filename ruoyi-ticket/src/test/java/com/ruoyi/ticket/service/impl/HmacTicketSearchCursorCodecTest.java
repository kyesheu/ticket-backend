package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.model.TicketSearchCursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 工单检索游标签名测试。 */
@DisplayName("工单检索游标签名测试")
class HmacTicketSearchCursorCodecTest {
    @Test
    void shouldRoundTripAndRejectTampering() {
        TicketSearchProperties properties = new TicketSearchProperties();
        properties.setCursorSecret("0123456789abcdef0123456789abcdef");
        HmacTicketSearchCursorCodec codec = new HmacTicketSearchCursorCodec(new ObjectMapper(), properties);
        TicketSearchCursor cursor = new TicketSearchCursor("hash", List.of("D:1.5", "L:9"));

        String encoded = codec.encode(cursor);

        assertThat(codec.decode(encoded)).isEqualTo(cursor);
        assertThatThrownBy(() -> codec.decode(encoded + "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("无效");
    }
}
