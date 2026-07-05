package com.ruoyi.ticket.util;

import java.util.regex.Pattern;

/**
 * AI 检索快照敏感信息脱敏器。
 */
public final class TicketAiSensitiveDataMasker {

    private static final String MASK = "[REDACTED]";
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)\\b(token|password|passwd|pwd)\\s*[:=]\\s*[^\\s,;]+"
    );

    private TicketAiSensitiveDataMasker() {
    }

    /** 脱敏手机号、邮箱、身份证号及常见凭据。 */
    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        String masked = MOBILE.matcher(value).replaceAll(MASK);
        masked = EMAIL.matcher(masked).replaceAll(MASK);
        masked = ID_CARD.matcher(masked).replaceAll(MASK);
        return SECRET.matcher(masked).replaceAll("$1=" + MASK);
    }
}
