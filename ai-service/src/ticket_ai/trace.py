"""traceId 中间件。"""

from uuid import uuid4

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

TRACE_ID_HEADER = "X-Trace-Id"
MAX_TRACE_ID_LENGTH = 128


class TraceIdMiddleware(BaseHTTPMiddleware):
    """回显调用方 traceId，缺失时生成新值。"""

    async def dispatch(self, request: Request, call_next) -> Response:
        trace_id = request.headers.get(TRACE_ID_HEADER)
        if not trace_id or len(trace_id) > MAX_TRACE_ID_LENGTH:
            trace_id = str(uuid4())
        response = await call_next(request)
        response.headers[TRACE_ID_HEADER] = trace_id
        return response
