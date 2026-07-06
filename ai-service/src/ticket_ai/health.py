"""AI 服务依赖健康检查。"""

from elasticsearch import Elasticsearch

from ticket_ai.config import Settings
from ticket_ai.models import HealthResponse


class HealthService:
    """只报告配置存在性和 Elasticsearch 可用性，不暴露密钥。"""

    def __init__(self, client: Elasticsearch, settings: Settings) -> None:
        self._client = client
        self._settings = settings

    def check(self) -> HealthResponse:
        try:
            available = bool(self._client.ping())
        except Exception:
            available = False
        embedding_configured = bool(self._settings.embedding_api_key and self._settings.embedding_model)
        llm_configured = bool(
            (self._settings.llm_api_key or self._settings.embedding_api_key) and self._settings.llm_model
        )
        status = "UP" if available and embedding_configured and llm_configured else "DEGRADED"
        return HealthResponse(
            status=status,
            elasticsearch_available=available,
            embedding_configured=embedding_configured,
            llm_configured=llm_configured,
        )
