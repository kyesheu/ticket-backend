"""v1 HTTP 契约测试。"""

import pytest
from httpx import ASGITransport, AsyncClient

from ticket_ai.config import Settings, get_settings
from ticket_ai.main import app

TEST_TOKEN = "test-service-token-12345"


def override_settings() -> Settings:
    return Settings(service_token=TEST_TOKEN)


app.dependency_overrides[get_settings] = override_settings


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
    assert response.json() == {"status": "UP", "contract_version": "v1"}


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
async def test_valid_business_request_reaches_stage_skeleton(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/tickets/assist",
        headers={"X-Service-Token": TEST_TOKEN},
        json={
            "contract_version": "v1",
            "ticket_no": "TK202607040001",
            "title": "Redis cache penetration",
            "description": "How should it be handled?",
            "category_name": "Middleware",
            "priority": "HIGH",
        },
    )

    assert response.status_code == 501
    assert response.json()["detail"] == "stage 47 not implemented"
