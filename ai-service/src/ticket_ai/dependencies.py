"""生产适配器装配。"""

from functools import lru_cache

from elasticsearch import Elasticsearch
from langchain_openai import OpenAIEmbeddings

from ticket_ai.config import get_settings
from ticket_ai.knowledge import DocumentImporter, ElasticsearchKnowledgeWriter
from ticket_ai.history_sync import (
    ClosedTicketSyncPreparationService,
    ClosedTicketSyncService,
    ElasticsearchClosedTicketWriter,
)
from ticket_ai.similar_search import SimilarKnowledgeSearchService, SimilarTicketSearchService


@lru_cache
def get_document_importer() -> DocumentImporter:
    """创建文档导入深模块。"""

    settings = get_settings()
    embeddings = OpenAIEmbeddings(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
        model=settings.embedding_model,
    )
    client = Elasticsearch(settings.elasticsearch_url)
    writer = ElasticsearchKnowledgeWriter(client, embeddings, settings.knowledge_index)
    return DocumentImporter(writer, max_bytes=settings.max_request_bytes)


@lru_cache
def get_closed_ticket_sync_service() -> ClosedTicketSyncService:
    """创建历史工单同步编排服务。"""

    settings = get_settings()
    embeddings = OpenAIEmbeddings(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
        model=settings.embedding_model,
    )
    client = Elasticsearch(settings.elasticsearch_url)
    preparation = ClosedTicketSyncPreparationService(embeddings, settings.embedding_model)
    writer = ElasticsearchClosedTicketWriter(client, settings.ticket_history_index)
    return ClosedTicketSyncService(preparation, writer)


@lru_cache
def get_similar_ticket_search_service() -> SimilarTicketSearchService:
    """创建历史工单相似检索服务。"""

    settings = get_settings()
    embeddings = OpenAIEmbeddings(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
        model=settings.embedding_model,
    )
    client = Elasticsearch(settings.elasticsearch_url)
    return SimilarTicketSearchService(embeddings, client, settings.ticket_history_index)


@lru_cache
def get_similar_knowledge_search_service() -> SimilarKnowledgeSearchService:
    """创建知识文档与历史工单统一检索服务。"""

    settings = get_settings()
    embeddings = OpenAIEmbeddings(
        api_key=settings.embedding_api_key,
        base_url=settings.embedding_base_url,
        model=settings.embedding_model,
    )
    client = Elasticsearch(settings.elasticsearch_url)
    return SimilarKnowledgeSearchService(
        embeddings, client, settings.knowledge_index, settings.ticket_history_index
    )
