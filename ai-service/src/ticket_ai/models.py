"""Java 与 Python 之间的 v1 HTTP 契约模型。"""

from datetime import datetime
from typing import Annotated, Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class StrictModel(BaseModel):
    """拒绝未知字段的契约基类。"""

    model_config = ConfigDict(extra="forbid")


class HealthResponse(StrictModel):
    status: Literal["UP", "DEGRADED"] = "UP"
    contract_version: Literal["v1"] = "v1"
    elasticsearch_available: bool
    embedding_configured: bool
    llm_configured: bool


class DocumentImportRequest(StrictModel):
    contract_version: Literal["v1"]
    source_id: str = Field(min_length=1, max_length=64)
    file_name: str = Field(min_length=1, max_length=255)
    content_type: str = Field(min_length=1, max_length=100)
    content_base64: str = Field(min_length=1)


class ClosedTicketSyncRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_id: int = Field(gt=0)
    title: str = Field(min_length=1, max_length=200)
    category: str = Field(min_length=1, max_length=100)
    description: str = Field(min_length=1, max_length=10000)
    solution: str = Field(min_length=1, max_length=10000)
    status: Literal["CLOSED"]
    tags: list[Annotated[str, Field(min_length=1, max_length=50)]] = Field(max_length=20)
    created_time: datetime
    closed_time: datetime
    source_generation: int = Field(gt=0)

    @model_validator(mode="after")
    def validate_times(self) -> "ClosedTicketSyncRequest":
        """关闭时间不得早于创建时间，且时间必须包含时区。"""

        if self.created_time.tzinfo is None or self.closed_time.tzinfo is None:
            raise ValueError("created_time and closed_time must include timezone")
        if self.closed_time < self.created_time:
            raise ValueError("closed_time must not precede created_time")
        if len(set(self.tags)) != len(self.tags):
            raise ValueError("tags must be unique")
        return self


class ClosedTicketSyncResponse(StrictModel):
    accepted: Literal[True] = True
    ticket_id: int = Field(gt=0)
    source_generation: int = Field(gt=0)


class TicketContextRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_no: str = Field(min_length=1, max_length=32)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1)
    category_name: str | None = Field(default=None, max_length=100)
    priority: str = Field(min_length=1, max_length=20)


class SourceItem(StrictModel):
    source_type: Literal["knowledge_document", "history_ticket"]
    source_id: str
    title: str
    snippet: str
    score: float = Field(ge=0)
    metadata: dict[str, Any]


class SearchResponse(StrictModel):
    sources: list[SourceItem]
    degraded: bool = False
    reason: str | None = None


class AssistRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_id: int = Field(gt=0)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1, max_length=10000)
    category: str | None = Field(default=None, max_length=100)
    top_k: int = Field(default=5, ge=1, le=20)


class AssistResponse(StrictModel):
    suggestion: str
    reply_draft: str
    sources: list[SourceItem]
    degraded: bool
    reason: str | None = None


class AcceptedResponse(StrictModel):
    accepted: Literal[True] = True


class DocumentImportResponse(StrictModel):
    accepted: Literal[True] = True
    chunk_count: int = Field(gt=0)
