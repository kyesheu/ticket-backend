"""历史工单同步准备服务测试。"""

from unittest.mock import Mock

import pytest
from langchain_core.embeddings import Embeddings

from ticket_ai.history_sync import ClosedTicketSyncPreparationService
from ticket_ai.models import ClosedTicketSyncRequest


def request() -> ClosedTicketSyncRequest:
    return ClosedTicketSyncRequest.model_validate({
        "contract_version": "v1",
        "ticket_id": 42,
        "title": "Redis 缓存穿透",
        "category": "中间件",
        "description": "不存在的 key 被反复查询",
        "solution": "参数校验、空值缓存和布隆过滤器",
        "status": "CLOSED",
        "tags": ["Redis", "缓存"],
        "created_time": "2026-07-01T09:00:00+08:00",
        "closed_time": "2026-07-01T10:00:00+08:00",
        "source_generation": 3,
    })


def test_builds_search_text_and_pending_elasticsearch_document() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = [[0.1, 0.2, 0.3]]
    service = ClosedTicketSyncPreparationService(embeddings)

    document = service.prepare(request())

    expected_text = "\n".join((
        "标题：Redis 缓存穿透",
        "分类：中间件",
        "问题描述：不存在的 key 被反复查询",
        "解决方案：参数校验、空值缓存和布隆过滤器",
        "标签：Redis、缓存",
    ))
    embeddings.embed_documents.assert_called_once_with([expected_text])
    assert document.document_id == "closed-ticket:42:3"
    assert document.source_type == "CLOSED_TICKET"
    assert document.source_id == "42"
    assert document.source_generation == 3
    assert document.title == "Redis 缓存穿透"
    assert document.content == expected_text
    assert document.vector == [0.1, 0.2, 0.3]
    assert document.metadata == {
        "ticket_id": 42,
        "category": "中间件",
        "status": "CLOSED",
        "tags": ["Redis", "缓存"],
        "created_time": "2026-07-01T09:00:00+08:00",
        "closed_time": "2026-07-01T10:00:00+08:00",
    }


def test_renders_empty_tags_without_dropping_field() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = [[1.0]]
    payload = request().model_copy(update={"tags": []})

    document = ClosedTicketSyncPreparationService(embeddings).prepare(payload)

    assert document.content.endswith("标签：无")
    assert document.metadata["tags"] == []


@pytest.mark.parametrize("vectors", [[], [[]], [[float("nan")]], [[float("inf")]], [[0.1], [0.2]]])
def test_rejects_invalid_embedding_response(vectors: list[list[float]]) -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = vectors

    with pytest.raises(ValueError, match="embedding response"):
        ClosedTicketSyncPreparationService(embeddings).prepare(request())
