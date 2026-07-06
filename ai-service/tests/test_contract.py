"""v1 HTTP 契约测试。"""

from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from httpx import ASGITransport, AsyncClient

from ticket_ai.config import Settings, get_settings
from ticket_ai.dependencies import (
    get_closed_ticket_sync_service,
    get_document_importer,
    get_similar_knowledge_search_service,
    get_ticket_assist_service,
    get_ticket_triage_service,
    get_health_service,
)
from ticket_ai.main import app

TEST_TOKEN = "test-service-token-12345"


def override_settings() -> Settings:
    return Settings(service_token=TEST_TOKEN, embedding_api_key="test-key", embedding_model="test-model")


app.dependency_overrides[get_settings] = override_settings
app.dependency_overrides[get_similar_knowledge_search_service] = Mock


class DefaultFakeHealthService:
    def check(self):
        return {
            "status": "UP", "contract_version": "v1", "elasticsearch_available": True,
            "embedding_configured": True, "llm_configured": True,
        }


app.dependency_overrides[get_health_service] = DefaultFakeHealthService


class DefaultFakeAssistService:
    def assist(self, request):
        assert request.ticket_id > 0
        return {
            "suggestion": "", "reply_draft": "", "sources": [],
            "degraded": True, "reason": "no_reliable_evidence",
        }


app.dependency_overrides[get_ticket_assist_service] = DefaultFakeAssistService


class DefaultFakeTriageService:
    def triage(self, request):
        assert request.ticket_id > 0
        return {
            "suggested_category_id": None, "suggested_priority": None, "suggested_assignee_id": None,
            "confidence": 0, "reason_summary": "", "sources": [],
            "degraded": True, "reason": "stage49_contract_only",
        }


app.dependency_overrides[get_ticket_triage_service] = DefaultFakeTriageService


@pytest.fixture
def anyio_backend() -> str:
    return "asyncio"


@pytest.fixture
async def client() -> AsyncClient:
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as value:
        yield value


@pytest.mark.anyio
async def test_health_exposes_v1_contract(client: AsyncClient) -> None:
    response = await client.get("/api/v1/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "UP", "contract_version": "v1", "elasticsearch_available": True,
        "embedding_configured": True, "llm_configured": True,
    }


@pytest.mark.anyio
async def test_business_endpoint_rejects_missing_credential(client: AsyncClient) -> None:
    response = await client.post("/api/v1/knowledge/search", json={})

    assert response.status_code == 401
    assert response.json()["detail"] == "invalid service credential"


@pytest.mark.anyio
async def test_business_endpoint_rejects_invalid_credential(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/knowledge/search",
        headers={"X-Service-Token": "wrong-service-token"},
        json={},
    )

    assert response.status_code == 401


@pytest.mark.anyio
async def test_unknown_contract_version_is_rejected(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/knowledge/search",
        headers={"X-Service-Token": TEST_TOKEN},
        json={
            "contract_version": "v2",
            "ticket_no": "TK202607040001",
            "title": "Redis cache penetration",
            "description": "How should it be handled?",
            "priority": "HIGH",
        },
    )

    assert response.status_code == 422


@pytest.mark.anyio
async def test_valid_assist_request_returns_structured_degraded_response(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/tickets/assist",
        headers={"X-Service-Token": TEST_TOKEN},
        json={
            "contract_version": "v1",
            "ticket_id": 42,
            "title": "Redis cache penetration",
            "description": "How should it be handled?",
            "category": "Middleware",
            "top_k": 5,
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "suggestion": "", "reply_draft": "", "sources": [],
        "degraded": True, "reason": "no_reliable_evidence",
    }


@pytest.mark.anyio
async def test_valid_triage_request_returns_structured_degraded_response(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/tickets/triage",
        headers={"X-Service-Token": TEST_TOKEN},
        json={
            "contract_version": "v1",
            "ticket_id": 42,
            "title": "WiFi unavailable",
            "description": "Office WiFi cannot connect",
            "current_category_id": 6,
            "current_category_name": "Network",
            "current_priority": "MEDIUM",
            "ticket_updated_at": "2026-07-06T12:00:00+08:00",
            "category_candidates": [{"category_id": 6, "category_name": "Network"}],
            "priority_candidates": ["LOW", "MEDIUM", "HIGH", "URGENT"],
            "assignee_candidates": [{"user_id": 1, "user_name": "admin", "nick_name": "Admin"}],
            "top_k": 5,
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "suggested_category_id": None, "suggested_priority": None, "suggested_assignee_id": None,
        "confidence": 0, "reason_summary": "", "sources": [],
        "degraded": True, "reason": "stage49_contract_only",
    }


@pytest.mark.anyio
async def test_triage_request_rejects_empty_candidates(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/tickets/triage",
        headers={"X-Service-Token": TEST_TOKEN},
        json={
            "contract_version": "v1",
            "ticket_id": 42,
            "title": "WiFi unavailable",
            "description": "Office WiFi cannot connect",
            "ticket_updated_at": "2026-07-06T12:00:00+08:00",
            "category_candidates": [],
            "priority_candidates": ["LOW"],
            "assignee_candidates": [{"user_id": 1, "user_name": "admin"}],
            "top_k": 5,
        },
    )

    assert response.status_code == 422


@pytest.mark.anyio
async def test_document_import_returns_chunk_count(client: AsyncClient) -> None:
    class FakeImporter:
        def import_document(self, source_id: str, file_name: str, content_type: str, encoded: str) -> int:
            assert (source_id, file_name, content_type, encoded) == ("doc-1", "a.txt", "text/plain", "YQ==")
            return 1

    app.dependency_overrides[get_document_importer] = lambda: FakeImporter()
    try:
        response = await client.post(
            "/api/v1/documents/import",
            headers={"X-Service-Token": TEST_TOKEN},
            json={"contract_version": "v1", "source_id": "doc-1", "file_name": "a.txt",
                  "content_type": "text/plain", "content_base64": "YQ=="},
        )
    finally:
        app.dependency_overrides.pop(get_document_importer, None)

    assert response.status_code == 200
    assert response.json() == {"accepted": True, "chunk_count": 1}


@pytest.mark.anyio
async def test_closed_ticket_sync_v1_contract_writes_through_service(client: AsyncClient) -> None:
    payload = {
        "contract_version": "v1", "ticket_id": 42, "title": "Redis 缓存穿透",
        "category": "中间件", "description": "不存在的 key 被反复查询",
        "solution": "参数校验、空值缓存和布隆过滤器", "status": "CLOSED",
        "tags": ["Redis", "缓存"], "created_time": "2026-07-01T09:00:00+08:00",
        "closed_time": "2026-07-01T10:00:00+08:00", "source_generation": 3,
    }

    class FakeSyncService:
        def sync(self, request):
            assert request.ticket_id == 42
            return SimpleNamespace(source_generation=3)

    app.dependency_overrides[get_closed_ticket_sync_service] = lambda: FakeSyncService()
    try:
        response = await client.post(
            "/api/v1/tickets/sync", headers={"X-Service-Token": TEST_TOKEN}, json=payload
        )
    finally:
        app.dependency_overrides.pop(get_closed_ticket_sync_service, None)

    assert response.status_code == 200
    assert response.json() == {"accepted": True, "ticket_id": 42, "source_generation": 3}


@pytest.mark.anyio
async def test_closed_ticket_sync_rejects_invalid_status(client: AsyncClient) -> None:
    app.dependency_overrides[get_closed_ticket_sync_service] = Mock
    try:
        response = await client.post(
            "/api/v1/tickets/sync",
            headers={"X-Service-Token": TEST_TOKEN},
            json={"contract_version": "v1", "ticket_id": 42, "status": "PROCESSING"},
        )
    finally:
        app.dependency_overrides.pop(get_closed_ticket_sync_service, None)

    assert response.status_code == 422


@pytest.mark.anyio
async def test_knowledge_search_combines_ticket_title_and_description(client: AsyncClient) -> None:
    result = SimpleNamespace(
        source_type="knowledge_document", source_id="doc-1", title="Redis 指南",
        snippet="使用布隆过滤器", score=1.7, metadata={"chunk_index": 0},
    )
    service = Mock()
    service.search.return_value = [result]
    app.dependency_overrides[get_similar_knowledge_search_service] = lambda: service
    try:
        response = await client.post(
            "/api/v1/knowledge/search",
            headers={"X-Service-Token": TEST_TOKEN},
            json={
                "contract_version": "v1", "ticket_no": "TK202607050001",
                "title": "Redis 缓存穿透", "description": "不存在的 key 被反复查询",
                "category_name": "中间件", "priority": "HIGH",
            },
        )
    finally:
        app.dependency_overrides[get_similar_knowledge_search_service] = Mock

    assert response.status_code == 200
    service.search.assert_called_once_with("Redis 缓存穿透\n不存在的 key 被反复查询")
    assert response.json()["sources"][0] == {
        "source_type": "knowledge_document", "source_id": "doc-1", "title": "Redis 指南",
        "snippet": "使用布隆过滤器", "score": 1.7, "metadata": {"chunk_index": 0},
    }
