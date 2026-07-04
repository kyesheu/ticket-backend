"""Java 与 Python 之间的 v1 HTTP 契约模型。"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class StrictModel(BaseModel):
    """拒绝未知字段的契约基类。"""

    model_config = ConfigDict(extra="forbid")


class HealthResponse(StrictModel):
    status: Literal["UP"] = "UP"
    contract_version: Literal["v1"] = "v1"


class DocumentImportRequest(StrictModel):
    contract_version: Literal["v1"]
    source_id: str = Field(min_length=1, max_length=64)
    file_name: str = Field(min_length=1, max_length=255)
    content_type: str = Field(min_length=1, max_length=100)
    content_base64: str = Field(min_length=1)


class ClosedTicketSyncRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_id: int = Field(gt=0)
    ticket_no: str = Field(min_length=1, max_length=32)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1)
    resolution: str = Field(min_length=1)


class TicketContextRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_no: str = Field(min_length=1, max_length=32)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1)
    category_name: str | None = Field(default=None, max_length=100)
    priority: str = Field(min_length=1, max_length=20)


class SourceItem(StrictModel):
    source_type: Literal["DOCUMENT", "CLOSED_TICKET"]
    source_id: str
    title: str
    snippet: str
    score: float = Field(ge=0, le=1)


class SearchResponse(StrictModel):
    sources: list[SourceItem]


class AssistResponse(StrictModel):
    suggestion: str
    reply_draft: str
    sources: list[SourceItem]


class AcceptedResponse(StrictModel):
    accepted: Literal[True] = True


class DocumentImportResponse(StrictModel):
    accepted: Literal[True] = True
    chunk_count: int = Field(gt=0)
