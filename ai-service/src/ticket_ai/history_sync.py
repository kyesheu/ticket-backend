"""历史已关闭工单同步准备服务。"""

import math
import hashlib
from dataclasses import dataclass

from elasticsearch import Elasticsearch
from langchain_core.embeddings import Embeddings

from ticket_ai.models import ClosedTicketSyncRequest


@dataclass(frozen=True)
class PendingElasticsearchDocument:
    """完成向量化、等待写入 Elasticsearch 的历史工单文档。"""

    document_id: str
    source_type: str
    source_id: str
    source_generation: int
    title: str
    category: str
    description: str
    solution: str
    tags: list[str]
    status: str
    content: str
    content_hash: str
    embedding: list[float]
    embedding_model: str
    created_time: str
    closed_time: str


class ClosedTicketSyncPreparationService:
    """将历史工单同步契约转换为待写入的向量文档。"""

    def __init__(self, embeddings: Embeddings, embedding_model: str) -> None:
        self._embeddings = embeddings
        self._embedding_model = embedding_model

    def prepare(self, request: ClosedTicketSyncRequest) -> PendingElasticsearchDocument:
        """组装检索文本、生成向量并返回待写入文档。"""

        content = self._build_search_text(request)
        vectors = self._embeddings.embed_documents([content])
        if len(vectors) != 1 or not vectors[0] or not all(math.isfinite(value) for value in vectors[0]):
            raise ValueError("embedding response must contain one finite non-empty vector")
        source_id = str(request.ticket_id)
        return PendingElasticsearchDocument(
            document_id=f"closed-ticket:{source_id}",
            source_type="CLOSED_TICKET",
            source_id=source_id,
            source_generation=request.source_generation,
            title=request.title,
            category=request.category,
            description=request.description,
            solution=request.solution,
            tags=list(request.tags),
            status=request.status,
            content=content,
            content_hash=hashlib.sha256(content.encode("utf-8")).hexdigest(),
            embedding=vectors[0],
            embedding_model=self._embedding_model,
            created_time=request.created_time.isoformat(),
            closed_time=request.closed_time.isoformat(),
        )

    def _build_search_text(self, request: ClosedTicketSyncRequest) -> str:
        tags = "、".join(request.tags) if request.tags else "无"
        return "\n".join((
            f"标题：{request.title}",
            f"分类：{request.category}",
            f"问题描述：{request.description}",
            f"解决方案：{request.solution}",
            f"标签：{tags}",
        ))


class ElasticsearchClosedTicketWriter:
    """使用稳定文档 ID 幂等写入独立历史工单索引。"""

    def __init__(self, client: Elasticsearch, index_name: str) -> None:
        self._client = client
        self._index_name = index_name

    def write(self, document: PendingElasticsearchDocument) -> None:
        """相同 ticket_id 覆盖同一文档，新代次自然替换旧代次。"""

        self._ensure_index(len(document.embedding))
        self._client.index(
            index=self._index_name,
            id=document.document_id,
            document={
                "ticket_id": int(document.source_id),
                "title": document.title,
                "category": document.category,
                "description": document.description,
                "solution": document.solution,
                "tags": document.tags,
                "status": document.status,
                "source_generation": document.source_generation,
                "content": document.content,
                "content_hash": document.content_hash,
                "embedding": document.embedding,
                "embedding_model": document.embedding_model,
                "created_time": document.created_time,
                "closed_time": document.closed_time,
            },
            refresh="wait_for",
            version=document.source_generation,
            version_type="external_gte",
        )

    def _ensure_index(self, dimensions: int) -> None:
        if self._client.indices.exists(index=self._index_name):
            return
        self._client.indices.create(
            index=self._index_name,
            mappings={
                "dynamic": "strict",
                "properties": {
                    "ticket_id": {"type": "long"},
                    "title": {"type": "text"},
                    "category": {"type": "keyword"},
                    "description": {"type": "text"},
                    "solution": {"type": "text"},
                    "tags": {"type": "keyword"},
                    "status": {"type": "keyword"},
                    "source_generation": {"type": "long"},
                    "content": {"type": "text"},
                    "content_hash": {"type": "keyword"},
                    "embedding": {
                        "type": "dense_vector", "dims": dimensions, "index": True, "similarity": "cosine"
                    },
                    "embedding_model": {"type": "keyword"},
                    "created_time": {"type": "date"},
                    "closed_time": {"type": "date"},
                },
            },
        )


class ClosedTicketSyncService:
    """编排历史工单向量化与 Elasticsearch 写入。"""

    def __init__(self, preparation: ClosedTicketSyncPreparationService,
                 writer: ElasticsearchClosedTicketWriter) -> None:
        self._preparation = preparation
        self._writer = writer

    def sync(self, request: ClosedTicketSyncRequest) -> PendingElasticsearchDocument:
        document = self._preparation.prepare(request)
        self._writer.write(document)
        return document
