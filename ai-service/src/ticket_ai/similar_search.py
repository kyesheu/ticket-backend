"""历史工单相似知识检索服务。"""

import math
from dataclasses import dataclass

from elasticsearch import Elasticsearch
from langchain_core.embeddings import Embeddings


@dataclass(frozen=True)
class SimilarTicketResult:
    """相似历史工单检索结果。"""

    ticket_id: int
    title: str
    category: str
    solution: str
    score: float
    source_generation: int


class SimilarTicketSearchService:
    """对查询生成向量并从独立历史工单索引召回结果。"""

    MAX_LIMIT = 20

    def __init__(self, embeddings: Embeddings, client: Elasticsearch, index_name: str) -> None:
        self._embeddings = embeddings
        self._client = client
        self._index_name = index_name

    def search(self, query: str, limit: int = 5) -> list[SimilarTicketResult]:
        """返回按 Elasticsearch 相似度排序的历史已关闭工单。"""

        normalized_query = query.strip()
        if not normalized_query:
            raise ValueError("query must not be blank")
        if limit <= 0 or limit > self.MAX_LIMIT:
            raise ValueError("limit must be between 1 and 20")
        vector = self._embeddings.embed_query(normalized_query)
        if not vector or not all(math.isfinite(value) for value in vector):
            raise ValueError("embedding response must be a finite non-empty vector")
        response = self._client.search(
            index=self._index_name,
            knn={
                "field": "embedding",
                "query_vector": vector,
                "k": limit,
                "num_candidates": max(50, limit * 10),
                "filter": {"term": {"status": "CLOSED"}},
            },
            source=["ticket_id", "title", "category", "solution", "source_generation"],
        )
        body = response.body if hasattr(response, "body") else response
        return [self._to_result(hit) for hit in body.get("hits", {}).get("hits", [])]

    def _to_result(self, hit: dict) -> SimilarTicketResult:
        source = hit.get("_source", {})
        required = ("ticket_id", "title", "category", "solution", "source_generation")
        if any(field not in source for field in required) or "_score" not in hit:
            raise ValueError("Elasticsearch hit misses required closed ticket fields")
        return SimilarTicketResult(
            ticket_id=int(source["ticket_id"]),
            title=str(source["title"]),
            category=str(source["category"]),
            solution=str(source["solution"]),
            score=float(hit["_score"]),
            source_generation=int(source["source_generation"]),
        )
