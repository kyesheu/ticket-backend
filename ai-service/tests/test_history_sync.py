"""历史工单同步准备服务测试。"""

from unittest.mock import Mock
from types import SimpleNamespace

import pytest
from langchain_core.embeddings import Embeddings

from ticket_ai.history_sync import (
    ClosedTicketSyncPreparationService,
    ClosedTicketSyncService,
    ElasticsearchClosedTicketWriter,
)
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
    service = ClosedTicketSyncPreparationService(embeddings, "text-embedding-test")

    document = service.prepare(request())

    expected_text = "\n".join((
        "标题：Redis 缓存穿透",
        "分类：中间件",
        "问题描述：不存在的 key 被反复查询",
        "解决方案：参数校验、空值缓存和布隆过滤器",
        "标签：Redis、缓存",
    ))
    embeddings.embed_documents.assert_called_once_with([expected_text])
    assert document.document_id == "closed-ticket:42"
    assert document.source_type == "CLOSED_TICKET"
    assert document.source_id == "42"
    assert document.source_generation == 3
    assert document.title == "Redis 缓存穿透"
    assert document.content == expected_text
    assert document.category == "中间件"
    assert document.description == "不存在的 key 被反复查询"
    assert document.solution == "参数校验、空值缓存和布隆过滤器"
    assert document.tags == ["Redis", "缓存"]
    assert document.status == "CLOSED"
    assert document.embedding == [0.1, 0.2, 0.3]
    assert document.embedding_model == "text-embedding-test"
    assert len(document.content_hash) == 64
    assert document.created_time == "2026-07-01T09:00:00+08:00"
    assert document.closed_time == "2026-07-01T10:00:00+08:00"


def test_renders_empty_tags_without_dropping_field() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = [[1.0]]
    payload = request().model_copy(update={"tags": []})

    document = ClosedTicketSyncPreparationService(embeddings, "test-model").prepare(payload)

    assert document.content.endswith("标签：无")
    assert document.tags == []


@pytest.mark.parametrize("vectors", [[], [[]], [[float("nan")]], [[float("inf")]], [[0.1], [0.2]]])
def test_rejects_invalid_embedding_response(vectors: list[list[float]]) -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = vectors

    with pytest.raises(ValueError, match="embedding response"):
        ClosedTicketSyncPreparationService(embeddings, "test-model").prepare(request())


def test_writes_all_required_fields_to_independent_index() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = [[0.1, 0.2]]
    document = ClosedTicketSyncPreparationService(embeddings, "test-model").prepare(request())
    client = Mock()
    client.indices.exists.return_value = False
    writer = ElasticsearchClosedTicketWriter(client, "ticket-history-test")

    writer.write(document)

    client.indices.create.assert_called_once()
    client.index.assert_called_once()
    call = client.index.call_args.kwargs
    assert call["index"] == "ticket-history-test"
    assert call["id"] == "closed-ticket:42"
    assert call["refresh"] == "wait_for"
    assert call["version"] == 3
    assert call["version_type"] == "external_gte"
    assert set(call["document"]) == {
        "ticket_id", "title", "category", "description", "solution", "tags", "status",
        "source_generation", "content", "content_hash", "embedding", "embedding_model",
        "created_time", "closed_time",
    }
    assert call["document"]["source_generation"] == 3
    assert call["document"]["embedding_model"] == "test-model"


def test_same_ticket_and_new_generation_use_same_elasticsearch_id() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_documents.return_value = [[0.1]]
    preparation = ClosedTicketSyncPreparationService(embeddings, "test-model")
    client = Mock()
    client.indices.exists.return_value = True
    writer = ElasticsearchClosedTicketWriter(client, "ticket-history-test")

    writer.write(preparation.prepare(request()))
    writer.write(preparation.prepare(request().model_copy(update={"source_generation": 4})))

    assert [call.kwargs["id"] for call in client.index.call_args_list] == [
        "closed-ticket:42", "closed-ticket:42"
    ]
    assert [call.kwargs["document"]["source_generation"] for call in client.index.call_args_list] == [3, 4]


def test_sync_service_prepares_then_writes_without_real_clients() -> None:
    preparation = Mock()
    writer = Mock()
    pending = SimpleNamespace(source_generation=3)
    preparation.prepare.return_value = pending

    result = ClosedTicketSyncService(preparation, writer).sync(request())

    assert result is pending
    preparation.prepare.assert_called_once()
    writer.write.assert_called_once_with(pending)
