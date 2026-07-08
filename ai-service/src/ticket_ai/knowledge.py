"""知识文档解析、切片与原子替换编排。"""

import base64
import binascii
import io
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import PurePath
from typing import Literal, Protocol

from elasticsearch import Elasticsearch, helpers
from langchain_core.embeddings import Embeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader

ALLOWED_TYPES = {
    ".txt": "text/plain",
    ".md": "text/markdown",
    ".pdf": "application/pdf",
}


class DocumentImportError(ValueError):
    """文档内容不满足导入约束。"""


@dataclass(frozen=True)
class KnowledgeChunk:
    """准备写入向量库的知识切片。"""

    source_id: str
    title: str
    chunk_index: int
    content: str


@dataclass(frozen=True)
class KnowledgeDocumentSummary:
    """知识文档管理列表项。"""

    source_id: str
    title: str
    status: Literal["ACTIVE", "IMPORTING", "FAILED", "DELETED"]
    chunk_count: int
    summary: str | None
    last_imported_at: datetime | None
    last_import_result: Literal["SUCCESS", "FAILED", "PENDING"] | None
    failure_reason_summary: str | None


class KnowledgeWriter(Protocol):
    """以来源为单位原子替换知识切片。"""

    def replace(self, source_id: str, chunks: list[KnowledgeChunk]) -> None: ...


class KnowledgeDocumentReader(Protocol):
    """读取知识文档运营元数据。"""

    def list_documents(self, page_num: int, page_size: int,
                       status: str | None = None) -> tuple[list[KnowledgeDocumentSummary], int]: ...

    def get_document(self, source_id: str) -> KnowledgeDocumentSummary | None: ...

    def delete_document(self, source_id: str) -> int: ...

    def reimport_document(self, source_id: str) -> int: ...


class KnowledgeDocumentOperationError(ValueError):
    """文档管理操作失败。"""


class DocumentImporter:
    """隐藏解析、切片和向量存储细节的导入模块。"""

    def __init__(self, writer: KnowledgeWriter, chunk_size: int = 800, chunk_overlap: int = 120,
                 max_bytes: int = 10_485_760) -> None:
        self._writer = writer
        self._max_bytes = max_bytes
        self._splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            length_function=len,
        )

    def import_document(self, source_id: str, file_name: str, content_type: str, encoded: str) -> int:
        extension = PurePath(file_name).suffix.lower()
        if ALLOWED_TYPES.get(extension) != content_type:
            raise DocumentImportError("unsupported document type")
        try:
            content = base64.b64decode(encoded, validate=True)
        except (binascii.Error, ValueError) as exception:
            raise DocumentImportError("invalid base64 content") from exception
        if not content:
            raise DocumentImportError("empty document")
        if len(content) > self._max_bytes:
            raise DocumentImportError("document exceeds size limit")
        text = self._extract(extension, content).strip()
        if not text:
            raise DocumentImportError("document contains no extractable text")
        pieces = self._splitter.split_text(text)
        chunks = [KnowledgeChunk(source_id, file_name, index, piece) for index, piece in enumerate(pieces)]
        self._writer.replace(source_id, chunks)
        return len(chunks)

    def _extract(self, extension: str, content: bytes) -> str:
        if extension in {".txt", ".md"}:
            try:
                return content.decode("utf-8")
            except UnicodeDecodeError as exception:
                raise DocumentImportError("text document must use UTF-8") from exception
        try:
            reader = PdfReader(io.BytesIO(content))
            if reader.is_encrypted:
                raise DocumentImportError("encrypted PDF is not supported")
            return "\n".join(page.extract_text() or "" for page in reader.pages)
        except DocumentImportError:
            raise
        except Exception as exception:
            raise DocumentImportError("invalid PDF document") from exception


class ElasticsearchKnowledgeWriter:
    """将完整切片集向量化后按来源切换为唯一有效代次。"""

    def __init__(self, client: Elasticsearch, embeddings: Embeddings, index_name: str) -> None:
        self._client = client
        self._embeddings = embeddings
        self._index_name = index_name

    def replace(self, source_id: str, chunks: list[KnowledgeChunk]) -> None:
        vectors = self._embeddings.embed_documents([chunk.content for chunk in chunks])
        if len(vectors) != len(chunks) or not vectors or not vectors[0]:
            raise RuntimeError("embedding response does not match chunks")
        self._ensure_index(len(vectors[0]))
        generation = uuid.uuid4().hex
        actions = [
            {
                "_op_type": "index",
                "_index": self._index_name,
                "_id": f"{source_id}:{generation}:{chunk.chunk_index}",
                "_source": {
                    "source_type": "DOCUMENT",
                    "source_id": source_id,
                    "title": chunk.title,
                    "chunk_index": chunk.chunk_index,
                    "content": chunk.content,
                    "generation": generation,
                    "active": False,
                    "vector": vector,
                },
            }
            for chunk, vector in zip(chunks, vectors, strict=True)
        ]
        helpers.bulk(self._client, actions, refresh="wait_for")
        self._client.update_by_query(
            index=self._index_name,
            query={"term": {"source_id": source_id}},
            script={
                "lang": "painless",
                "source": "ctx._source.active = ctx._source.generation == params.generation",
                "params": {"generation": generation},
            },
            refresh=True,
            conflicts="proceed",
        )
        self._client.delete_by_query(
            index=self._index_name,
            query={"bool": {"filter": [
                {"term": {"source_id": source_id}}, {"term": {"active": False}}
            ]}},
            refresh=True,
            conflicts="proceed",
        )

    def _ensure_index(self, dimensions: int) -> None:
        if self._client.indices.exists(index=self._index_name):
            return
        self._client.indices.create(
            index=self._index_name,
            mappings={
                "dynamic": "strict",
                "properties": {
                    "source_type": {"type": "keyword"},
                    "source_id": {"type": "keyword"},
                    "title": {"type": "text"},
                    "chunk_index": {"type": "integer"},
                    "content": {"type": "text"},
                    "generation": {"type": "keyword"},
                    "active": {"type": "boolean"},
                    "vector": {"type": "dense_vector", "dims": dimensions, "index": True, "similarity": "cosine"},
                },
            },
        )


class ElasticsearchKnowledgeDocumentReader:
    """从 Elasticsearch 有效切片聚合知识文档管理元数据。"""

    def __init__(self, client: Elasticsearch, index_name: str) -> None:
        self._client = client
        self._index_name = index_name

    def list_documents(self, page_num: int, page_size: int,
                       status: str | None = None) -> tuple[list[KnowledgeDocumentSummary], int]:
        if page_num < 1 or page_size < 1 or page_size > 100:
            raise ValueError("invalid pagination")
        if status not in {None, "ACTIVE"}:
            return [], 0
        if not self._client.indices.exists(index=self._index_name):
            return [], 0
        start = (page_num - 1) * page_size
        response = self._client.search(
            index=self._index_name,
            size=0,
            query={"term": {"active": True}},
            aggs={
                "documents": {
                    "terms": {"field": "source_id", "size": start + page_size, "order": {"_key": "asc"}},
                    "aggs": {
                        "title": {"top_hits": {"size": 1, "_source": ["title"]}},
                        "chunk_count": {"value_count": {"field": "chunk_index"}},
                    },
                }
            },
        )
        buckets = response.get("aggregations", {}).get("documents", {}).get("buckets", [])
        rows = [self._bucket_to_summary(bucket) for bucket in buckets[start:start + page_size]]
        return rows, len(buckets)

    def get_document(self, source_id: str) -> KnowledgeDocumentSummary | None:
        if not source_id or not self._client.indices.exists(index=self._index_name):
            return None
        response = self._client.search(
            index=self._index_name,
            size=0,
            query={"bool": {"filter": [{"term": {"active": True}}, {"term": {"source_id": source_id}}]}},
            aggs={
                "documents": {
                    "terms": {"field": "source_id", "size": 1},
                    "aggs": {
                        "title": {"top_hits": {"size": 1, "_source": ["title"]}},
                        "chunk_count": {"value_count": {"field": "chunk_index"}},
                    },
                }
            },
        )
        buckets = response.get("aggregations", {}).get("documents", {}).get("buckets", [])
        return self._bucket_to_summary(buckets[0]) if buckets else None

    def delete_document(self, source_id: str) -> int:
        if not source_id:
            raise KnowledgeDocumentOperationError("invalid source id")
        if not self._client.indices.exists(index=self._index_name):
            return 0
        document = self.get_document(source_id)
        if document is None:
            return 0
        response = self._client.delete_by_query(
            index=self._index_name,
            query={"term": {"source_id": source_id}},
            refresh=True,
            conflicts="proceed",
        )
        return int(response.get("deleted", 0))

    def reimport_document(self, source_id: str) -> int:
        document = self.get_document(source_id)
        if document is None:
            raise KnowledgeDocumentOperationError("document not found")
        return document.chunk_count

    def _bucket_to_summary(self, bucket: dict) -> KnowledgeDocumentSummary:
        title_hits = bucket.get("title", {}).get("hits", {}).get("hits", [])
        title = title_hits[0].get("_source", {}).get("title", bucket["key"]) if title_hits else bucket["key"]
        return KnowledgeDocumentSummary(
            source_id=bucket["key"],
            title=title,
            status="ACTIVE",
            chunk_count=int(bucket.get("chunk_count", {}).get("value", bucket.get("doc_count", 0))),
            summary=None,
            last_imported_at=datetime.now(timezone.utc),
            last_import_result="SUCCESS",
            failure_reason_summary=None,
        )
