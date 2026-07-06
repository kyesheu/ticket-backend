"""知识文档与历史工单统一检索测试。"""

from unittest.mock import Mock

from langchain_core.embeddings import Embeddings

from ticket_ai.similar_search import SimilarKnowledgeSearchService


def test_returns_document_and_history_hits_with_source_metadata() -> None:
    embeddings = Mock(spec=Embeddings)
    embeddings.embed_query.return_value = [1.0, 0.0]
    client = Mock()
    client.search.side_effect = [
        {"hits": {"hits": [{"_score": 1.8, "_source": {
            "source_id": "redis-guide", "title": "Redis 指南", "content": "使用布隆过滤器",
            "chunk_index": 2, "generation": "g1",
        }}]}},
        {"hits": {"hits": [{"_score": 1.6, "_source": {
            "ticket_id": 42, "title": "缓存穿透", "solution": "缓存空值",
            "category": "中间件", "source_generation": 3,
        }}]}},
    ]
    service = SimilarKnowledgeSearchService(
        embeddings, client, "ticket-knowledge-test", "ticket-history-test"
    )

    results = service.search("Redis 缓存穿透", 5)

    embeddings.embed_query.assert_called_once_with("Redis 缓存穿透")
    assert [item.source_type for item in results] == ["knowledge_document", "history_ticket"]
    assert results[0].snippet == "使用布隆过滤器"
    assert results[0].score == 1.8
    assert results[0].metadata == {"chunk_index": 2, "generation": "g1"}
    assert results[1].snippet == "缓存空值"
    assert results[1].metadata == {"category": "中间件", "source_generation": 3}
