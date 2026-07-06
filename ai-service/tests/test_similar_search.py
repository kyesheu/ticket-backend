"""历史工单相似检索服务测试。"""

from unittest.mock import Mock

import pytest
from langchain_core.embeddings import Embeddings

from ticket_ai.similar_search import SimilarTicketSearchService


def service(response: dict, vector: list[float] | None = None) -> tuple[SimilarTicketSearchService, Mock, Mock]:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_query.return_value = [0.1, 0.2] if vector is None else vector
    client = Mock()
    client.search.return_value = response
    return SimilarTicketSearchService(embeddings, client, "ticket-history-test"), embeddings, client


def test_embeds_query_and_maps_similar_closed_tickets() -> None:
    search_service, embeddings, client = service({
        "hits": {"hits": [{
            "_score": 1.82,
            "_source": {
                "ticket_id": 42,
                "title": "Redis 缓存穿透",
                "category": "中间件",
                "solution": "空值缓存和布隆过滤器",
                "source_generation": 3,
            },
        }]},
    })

    result = search_service.search("  Redis 缓存穿透怎么处理？  ", limit=3)

    embeddings.embed_query.assert_called_once_with("Redis 缓存穿透怎么处理？")
    call = client.search.call_args.kwargs
    assert call["index"] == "ticket-history-test"
    assert call["knn"] == {
        "field": "embedding",
        "query_vector": [0.1, 0.2],
        "k": 3,
        "num_candidates": 50,
        "filter": {"term": {"status": "CLOSED"}},
    }
    assert call["source"] == ["ticket_id", "title", "category", "solution", "source_generation"]
    assert len(result) == 1
    assert result[0].ticket_id == 42
    assert result[0].title == "Redis 缓存穿透"
    assert result[0].category == "中间件"
    assert result[0].solution == "空值缓存和布隆过滤器"
    assert result[0].score == 1.82
    assert result[0].source_generation == 3


def test_returns_empty_list_when_elasticsearch_has_no_hits() -> None:
    search_service, _, _ = service({"hits": {"hits": []}})

    assert search_service.search("Redis") == []


@pytest.mark.parametrize("query", ["", "   "])
def test_rejects_blank_query_without_external_calls(query: str) -> None:
    search_service, embeddings, client = service({})

    with pytest.raises(ValueError, match="blank"):
        search_service.search(query)

    embeddings.embed_query.assert_not_called()
    client.search.assert_not_called()


@pytest.mark.parametrize("limit", [0, 21])
def test_rejects_invalid_limit(limit: int) -> None:
    search_service, embeddings, client = service({})

    with pytest.raises(ValueError, match="limit"):
        search_service.search("Redis", limit)

    embeddings.embed_query.assert_not_called()
    client.search.assert_not_called()


@pytest.mark.parametrize("vector", [[], [float("nan")], [float("inf")]])
def test_rejects_invalid_embedding_without_search(vector: list[float]) -> None:
    search_service, _, client = service({}, vector)

    with pytest.raises(ValueError, match="embedding"):
        search_service.search("Redis")

    client.search.assert_not_called()


def test_rejects_hit_missing_required_fields() -> None:
    search_service, _, _ = service({"hits": {"hits": [{"_score": 1.0, "_source": {"ticket_id": 42}}]}})

    with pytest.raises(ValueError, match="required"):
        search_service.search("Redis")
