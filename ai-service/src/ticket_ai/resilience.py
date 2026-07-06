"""AI 接口基础故障隔离与限流。"""

import threading
import time

from fastapi import Depends, HTTPException, status

from ticket_ai.config import Settings, get_settings


class RetrievalUnavailable(RuntimeError):
    """检索依赖不可用。"""


class AiRateLimiter:
    """进程内固定窗口限流器，仅用于 AI 辅助接口。"""

    def __init__(self, limit: int, window_seconds: int) -> None:
        self._limit = limit
        self._window_seconds = window_seconds
        self._window_started = time.monotonic()
        self._count = 0
        self._lock = threading.Lock()

    def check(self) -> None:
        with self._lock:
            now = time.monotonic()
            if now - self._window_started >= self._window_seconds:
                self._window_started = now
                self._count = 0
            if self._count >= self._limit:
                raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                                    detail="AI request rate limit exceeded")
            self._count += 1


_limiter: AiRateLimiter | None = None


def verify_ai_rate_limit(settings: Settings = Depends(get_settings)) -> None:
    """按配置执行 AI 辅助接口限流。"""

    global _limiter
    if _limiter is None:
        _limiter = AiRateLimiter(settings.assist_rate_limit, settings.assist_rate_window_seconds)
    _limiter.check()
