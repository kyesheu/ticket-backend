"""真实 Elasticsearch 历史工单相似检索 smoke。"""

import os
import sys
import uuid

from elasticsearch import Elasticsearch
from langchain_core.embeddings import Embeddings

from ticket_ai.similar_search import SimilarTicketSearchService


class FakeEmbeddings(Embeddings):
    """为 smoke 提供确定性查询向量，不调用外部 Embedding 服务。"""

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [[1.0, 0.0, 0.0] for _ in texts]

    def embed_query(self, text: str) -> list[float]:
        del text
        return [1.0, 0.0, 0.0]


def create_index(client: Elasticsearch, index_name: str) -> None:
    """创建仅供本次 smoke 使用的向量索引。"""

    client.indices.create(
        index=index_name,
        mappings={
            "dynamic": "strict",
            "properties": {
                "ticket_id": {"type": "long"},
                "title": {"type": "text"},
                "category": {"type": "keyword"},
                "solution": {"type": "text"},
                "status": {"type": "keyword"},
                "source_generation": {"type": "long"},
                "embedding": {
                    "type": "dense_vector",
                    "dims": 3,
                    "index": True,
                    "similarity": "cosine",
                },
            },
        },
        settings={"number_of_shards": 1, "number_of_replicas": 0},
    )


def write_fixtures(client: Elasticsearch, index_name: str) -> None:
    """写入一条 Redis 工单和一条数据库工单。"""

    fixtures = (
        {
            "ticket_id": 101,
            "title": "Redis 缓存穿透",
            "category": "中间件",
            "solution": "使用空值缓存和布隆过滤器",
            "status": "CLOSED",
            "source_generation": 1,
            "embedding": [1.0, 0.0, 0.0],
        },
        {
            "ticket_id": 102,
            "title": "MySQL 慢查询",
            "category": "数据库",
            "solution": "补充联合索引并检查执行计划",
            "status": "CLOSED",
            "source_generation": 1,
            "embedding": [0.0, 1.0, 0.0],
        },
    )
    for document in fixtures:
        client.index(index=index_name, id=f"closed-ticket:{document['ticket_id']}", document=document)
    client.indices.refresh(index=index_name)


def main() -> int:
    elasticsearch_url = os.getenv("TICKET_AI_ELASTICSEARCH_URL", "http://127.0.0.1:9200")
    index_name = f"ticket-history-smoke-{uuid.uuid4().hex}"
    client = Elasticsearch(elasticsearch_url, request_timeout=10)
    created = False
    exit_code = 0
    success_message = ""
    try:
        client.info()
        create_index(client, index_name)
        created = True
        write_fixtures(client, index_name)
        service = SimilarTicketSearchService(FakeEmbeddings(), client, index_name)
        results = service.search("Redis 缓存穿透怎么处理？", limit=2)
        assert len(results) == 2, f"expected 2 results, got {len(results)}"
        assert results[0].ticket_id == 101, f"expected ticket 101 first, got {results[0].ticket_id}"
        success_message = f"PASS: most similar ticket_id={results[0].ticket_id}, index={index_name}"
    except Exception as exception:
        print(
            "ERROR: Elasticsearch unavailable or similar-search smoke failed "
            f"({type(exception).__name__}). Check TICKET_AI_ELASTICSEARCH_URL and Elasticsearch health.",
            file=sys.stderr,
        )
        exit_code = 1
    finally:
        if created:
            try:
                client.indices.delete(index=index_name)
            except Exception as exception:
                print(
                    f"ERROR: failed to clean smoke index {index_name} ({type(exception).__name__}).",
                    file=sys.stderr,
                )
                exit_code = 1
        client.close()
    if exit_code == 0:
        print(success_message)
        print(f"PASS: cleaned smoke index {index_name}")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
