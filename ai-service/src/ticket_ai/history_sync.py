"""历史已关闭工单同步准备服务。"""

import math
from dataclasses import dataclass

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
    content: str
    vector: list[float]
    metadata: dict[str, object]


class ClosedTicketSyncPreparationService:
    """将历史工单同步契约转换为待写入的向量文档。"""

    def __init__(self, embeddings: Embeddings) -> None:
        self._embeddings = embeddings

    def prepare(self, request: ClosedTicketSyncRequest) -> PendingElasticsearchDocument:
        """组装检索文本、生成向量并返回待写入文档。"""

        content = self._build_search_text(request)
        vectors = self._embeddings.embed_documents([content])
        if len(vectors) != 1 or not vectors[0] or not all(math.isfinite(value) for value in vectors[0]):
            raise ValueError("embedding response must contain one finite non-empty vector")
        source_id = str(request.ticket_id)
        return PendingElasticsearchDocument(
            document_id=f"closed-ticket:{source_id}:{request.source_generation}",
            source_type="CLOSED_TICKET",
            source_id=source_id,
            source_generation=request.source_generation,
            title=request.title,
            content=content,
            vector=vectors[0],
            metadata={
                "ticket_id": request.ticket_id,
                "category": request.category,
                "status": request.status,
                "tags": list(request.tags),
                "created_time": request.created_time.isoformat(),
                "closed_time": request.closed_time.isoformat(),
            },
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
