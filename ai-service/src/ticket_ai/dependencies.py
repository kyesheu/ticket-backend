"""生产适配器装配。"""

from functools import lru_cache
import hashlib
import json
import re

from elasticsearch import Elasticsearch
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_core.embeddings import Embeddings
from langchain_core.runnables import RunnableLambda

from ticket_ai.assist import TicketAssistService
from ticket_ai.config import get_settings
from ticket_ai.knowledge import DocumentImporter, ElasticsearchKnowledgeWriter
from ticket_ai.history_sync import (
    ClosedTicketSyncPreparationService,
    ClosedTicketSyncService,
    ElasticsearchClosedTicketWriter,
)
from ticket_ai.health import HealthService
from ticket_ai.similar_search import SimilarKnowledgeSearchService, SimilarTicketSearchService


class SmokeEmbeddings(Embeddings):
    """仅供隔离 smoke 索引使用的确定性本地向量。"""

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._vector(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._vector(text)

    def _vector(self, text: str) -> list[float]:
        digest = hashlib.sha256(text.encode("utf-8")).digest()
        return [(value / 127.5) - 1.0 for value in digest[:16]]


def _create_embeddings(settings):
    if settings.smoke_mode:
        return SmokeEmbeddings()
    return OpenAIEmbeddings(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
        model=settings.embedding_model,
        request_timeout=settings.external_timeout_seconds,
    )


def _smoke_llm(prompt):
    match = re.search(r"允许来源 ID：([^\n]+)", prompt.to_string())
    source_id = match.group(1).split(",")[0].strip() if match else ""
    return json.dumps({
        "suggestion": "根据检索证据检查配置并执行验证。",
        "reply_draft": "您好，我们已根据知识库内容整理处理方案，请确认后执行。",
        "source_ids": [source_id],
    }, ensure_ascii=False)


@lru_cache
def get_document_importer() -> DocumentImporter:
    """创建文档导入深模块。"""

    settings = get_settings()
    embeddings = _create_embeddings(settings)
    client = Elasticsearch(settings.elasticsearch_url, request_timeout=settings.external_timeout_seconds)
    writer = ElasticsearchKnowledgeWriter(client, embeddings, settings.knowledge_index)
    return DocumentImporter(writer, max_bytes=settings.max_request_bytes)


@lru_cache
def get_closed_ticket_sync_service() -> ClosedTicketSyncService:
    """创建历史工单同步编排服务。"""

    settings = get_settings()
    embeddings = _create_embeddings(settings)
    client = Elasticsearch(settings.elasticsearch_url, request_timeout=settings.external_timeout_seconds)
    preparation = ClosedTicketSyncPreparationService(embeddings, settings.embedding_model)
    writer = ElasticsearchClosedTicketWriter(client, settings.ticket_history_index)
    return ClosedTicketSyncService(preparation, writer)


@lru_cache
def get_similar_ticket_search_service() -> SimilarTicketSearchService:
    """创建历史工单相似检索服务。"""

    settings = get_settings()
    embeddings = _create_embeddings(settings)
    client = Elasticsearch(settings.elasticsearch_url, request_timeout=settings.external_timeout_seconds)
    return SimilarTicketSearchService(embeddings, client, settings.ticket_history_index)


@lru_cache
def get_similar_knowledge_search_service() -> SimilarKnowledgeSearchService:
    """创建知识文档与历史工单统一检索服务。"""

    settings = get_settings()
    embeddings = _create_embeddings(settings)
    client = Elasticsearch(settings.elasticsearch_url, request_timeout=settings.external_timeout_seconds)
    return SimilarKnowledgeSearchService(
        embeddings, client, settings.knowledge_index, settings.ticket_history_index
    )


@lru_cache
def get_ticket_assist_service() -> TicketAssistService:
    """创建工单 RAG 辅助服务。"""

    settings = get_settings()
    llm = RunnableLambda(_smoke_llm) if settings.smoke_mode else ChatOpenAI(
        api_key=settings.llm_api_key or settings.embedding_api_key,
        base_url=settings.llm_base_url or settings.embedding_base_url,
        model=settings.llm_model,
        timeout=settings.llm_timeout_seconds,
        temperature=0,
    )
    return TicketAssistService(get_similar_knowledge_search_service(), llm)


@lru_cache
def get_health_service() -> HealthService:
    """创建依赖健康检查服务。"""

    settings = get_settings()
    client = Elasticsearch(settings.elasticsearch_url, request_timeout=settings.external_timeout_seconds)
    return HealthService(client, settings)
