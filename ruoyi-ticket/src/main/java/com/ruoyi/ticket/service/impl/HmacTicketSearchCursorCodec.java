package com.ruoyi.ticket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.config.TicketSearchProperties;
import com.ruoyi.ticket.model.TicketSearchCursor;
import com.ruoyi.ticket.service.TicketSearchCursorCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/** HMAC-SHA256 工单检索游标编解码器。 */
@Component
@ConditionalOnProperty(prefix = "ticket.search", name = "enabled", havingValue = "true")
public class HmacTicketSearchCursorCodec implements TicketSearchCursorCodec {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public HmacTicketSearchCursorCodec(ObjectMapper objectMapper, TicketSearchProperties properties) {
        if (StringUtils.isBlank(properties.getCursorSecret())
                || properties.getCursorSecret().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("ticket.search.cursor-secret 至少需要 32 个字符");
        }
        this.objectMapper = objectMapper;
        this.secret = properties.getCursorSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String encode(TicketSearchCursor cursor) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cursor));
            return payload + "." + sign(payload);
        } catch (JsonProcessingException exception) {
            throw new ServiceException("检索游标生成失败");
        }
    }

    @Override
    public TicketSearchCursor decode(String encodedCursor) {
        try {
            String[] parts = encodedCursor.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(
                    sign(parts[0]).getBytes(StandardCharsets.US_ASCII),
                    parts[1].getBytes(StandardCharsets.US_ASCII))) {
                throw new ServiceException("检索游标无效");
            }
            return objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), TicketSearchCursor.class);
        } catch (IllegalArgumentException | IOException exception) {
            throw new ServiceException("检索游标无效");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("检索游标签名初始化失败", exception);
        }
    }
}
