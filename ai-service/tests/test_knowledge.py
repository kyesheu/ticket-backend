"""知识文档导入模块测试。"""

import base64

import pytest

from ticket_ai.knowledge import DocumentImporter, DocumentImportError, KnowledgeChunk


class RecordingWriter:
    def __init__(self) -> None:
        self.calls: list[tuple[str, list[KnowledgeChunk]]] = []

    def replace(self, source_id: str, chunks: list[KnowledgeChunk]) -> None:
        self.calls.append((source_id, chunks))


def encoded(value: bytes) -> str:
    return base64.b64encode(value).decode("ascii")


def test_imports_utf8_text_and_splits_content() -> None:
    writer = RecordingWriter()
    importer = DocumentImporter(writer, chunk_size=20, chunk_overlap=5)

    content = ("缓存穿透处理方案" * 8).encode("utf-8")
    count = importer.import_document("doc-1", "redis.md", "text/markdown", encoded(content))

    assert count > 1
    assert writer.calls[0][0] == "doc-1"
    assert [chunk.chunk_index for chunk in writer.calls[0][1]] == list(range(count))


@pytest.mark.parametrize(
    ("file_name", "content_type", "content"),
    [
        ("empty.txt", "text/plain", b""),
        ("blank.md", "text/markdown", b"   \n"),
        ("script.exe", "application/octet-stream", b"data"),
        ("fake.pdf", "text/plain", b"data"),
    ],
)
def test_rejects_invalid_documents(file_name: str, content_type: str, content: bytes) -> None:
    writer = RecordingWriter()

    with pytest.raises(DocumentImportError):
        DocumentImporter(writer).import_document("doc-1", file_name, content_type, encoded(content))

    assert writer.calls == []


def test_rejects_invalid_base64() -> None:
    with pytest.raises(DocumentImportError, match="base64"):
        DocumentImporter(RecordingWriter()).import_document("doc-1", "a.txt", "text/plain", "***")


def test_rejects_document_above_size_limit() -> None:
    importer = DocumentImporter(RecordingWriter(), max_bytes=3)

    with pytest.raises(DocumentImportError, match="size"):
        importer.import_document("doc-1", "a.txt", "text/plain", encoded(b"1234"))
