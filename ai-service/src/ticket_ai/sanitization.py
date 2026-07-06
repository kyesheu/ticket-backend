"""日志和诊断文本脱敏。"""

import re

MASK = "[REDACTED]"
PATTERNS = (
    re.compile(r"(?<!\d)1[3-9]\d{9}(?!\d)"),
    re.compile(r"[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}", re.IGNORECASE),
    re.compile(r"(?<!\d)\d{17}[0-9Xx](?!\d)"),
    re.compile(r"\b(api_key|token|password|authorization)\s*[:=]\s*[^\s,;]+", re.IGNORECASE),
)


def sanitize_text(value: str) -> str:
    """移除常见凭据和个人敏感信息。"""

    result = value
    for pattern in PATTERNS:
        result = pattern.sub(lambda match: f"{match.group(1)}={MASK}" if match.lastindex else MASK, result)
    return result
