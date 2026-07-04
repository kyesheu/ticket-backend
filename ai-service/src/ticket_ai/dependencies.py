"""生产适配器装配。"""

from functools import lru_cache

from elasticsearch import Elasticsearch
from langchain_openai import OpenAIEmbeddings

from ticket_ai.config import get_settings
from ticket_ai.knowledge import DocumentImporter, ElasticsearchKnowledgeWriter


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
