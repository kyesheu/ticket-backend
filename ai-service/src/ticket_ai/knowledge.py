"""知识文档解析、切片与原子替换编排。"""

import base64
import binascii
import io
import uuid
from dataclasses import dataclass
from pathlib import PurePath
from typing import Protocol

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


class KnowledgeWriter(Protocol):
    """以来源为单位原子替换知识切片。"""

    def replace(self, source_id: str, chunks: list[KnowledgeChunk]) -> None: ...


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
