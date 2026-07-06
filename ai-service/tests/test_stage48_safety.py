"""阶段四十八安全、健康与固定评测集测试。"""

import json
from pathlib import Path
from unittest.mock import Mock

import pytest
from fastapi import HTTPException
from langchain_core.embeddings import Embeddings

from ticket_ai.config import Settings
from ticket_ai.health import HealthService
from ticket_ai.resilience import AiRateLimiter, RetrievalUnavailable
from ticket_ai.sanitization import sanitize_text
from ticket_ai.similar_search import SimilarKnowledgeSearchService


def test_fixed_evaluation_set_has_required_six_scenarios() -> None:
    cases = json.loads((Path(__file__).parent / "fixtures" / "evaluation_cases.json").read_text("utf-8"))
    assert len(cases) >= 6
    assert {case["id"] for case in cases} >= {
        "knowledge-hit", "history-hit", "mixed-hit", "no-evidence",
        "prompt-injection", "forged-reference",
    }
    assert all(set(case) == {"id", "expected", "safety"} for case in cases)


def test_smoke_mode_requires_isolated_indexes() -> None:
    with pytest.raises(ValueError, match="smoke/test indexes"):
        Settings(service_token="service-token-1234", embedding_api_key="key",
                 embedding_model="embed", smoke_mode=True)

    settings = Settings(
        service_token="service-token-1234", embedding_api_key="key", embedding_model="embed",
        smoke_mode=True, knowledge_index="ticket-knowledge-smoke", ticket_history_index="ticket-history-smoke",
    )
    assert settings.smoke_mode is True


def test_health_reports_dependencies_without_exposing_secrets() -> None:
    client = Mock()
    client.ping.return_value = True
    settings = Settings(service_token="service-token-1234", embedding_api_key="secret-key",
                        embedding_model="embed", llm_model="llm")

    result = HealthService(client, settings).check()

    assert result.model_dump() == {
        "status": "UP", "contract_version": "v1", "elasticsearch_available": True,
        "embedding_configured": True, "llm_configured": True,
    }
    assert "secret" not in result.model_dump_json()


def test_health_degrades_when_elasticsearch_is_unavailable() -> None:
    client = Mock()
    client.ping.side_effect = TimeoutError()
    settings = Settings(service_token="service-token-1234", embedding_api_key="key",
                        embedding_model="embed")
    result = HealthService(client, settings).check()
    assert result.status == "DEGRADED"
    assert result.elasticsearch_available is False


def test_rate_limiter_rejects_request_over_limit() -> None:
    limiter = AiRateLimiter(1, 60)
    limiter.check()
    with pytest.raises(HTTPException) as error:
        limiter.check()
    assert error.value.status_code == 429


def test_sensitive_log_text_is_redacted() -> None:
    value = ("api_key=abc token=def password=ghi Authorization=Bearer-secret "
             "13812345678 admin@example.com 11010519491231002X")
    sanitized = sanitize_text(value)
    for secret in ("abc", "def", "ghi", "Bearer-secret", "13812345678",
                   "admin@example.com", "11010519491231002X"):
        assert secret not in sanitized


def test_embedding_failure_has_explicit_degraded_reason() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_query.side_effect = TimeoutError()
    service = SimilarKnowledgeSearchService(embeddings, Mock(), "knowledge", "history")
    with pytest.raises(RetrievalUnavailable, match="embedding_unavailable"):
        service.search("Redis")


def test_elasticsearch_failure_has_explicit_degraded_reason() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_query.return_value = [0.1]
    client = Mock()
    client.search.side_effect = TimeoutError()
    service = SimilarKnowledgeSearchService(embeddings, client, "knowledge", "history")
    with pytest.raises(RetrievalUnavailable, match="vector_store_unavailable"):
        service.search("Redis")
