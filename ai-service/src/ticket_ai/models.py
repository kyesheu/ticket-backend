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


class TriageCategoryCandidate(StrictModel):
    category_id: int = Field(gt=0)
    category_name: str = Field(min_length=1, max_length=100)


class TriageAssigneeCandidate(StrictModel):
    user_id: int = Field(gt=0)
    user_name: str = Field(min_length=1, max_length=30)
    nick_name: str | None = Field(default=None, max_length=30)
    dept_id: int | None = Field(default=None, gt=0)
    dept_name: str | None = Field(default=None, max_length=30)
    workload_score: float | None = Field(default=None, ge=0, le=1)


class TriageRequest(StrictModel):
    contract_version: Literal["v1"]
    ticket_id: int = Field(gt=0)
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1, max_length=10000)
    current_category_id: int | None = Field(default=None, gt=0)
    current_category_name: str | None = Field(default=None, max_length=100)
    current_priority: str | None = Field(default=None, max_length=20)
    ticket_updated_at: datetime
    category_candidates: list[TriageCategoryCandidate] = Field(min_length=1, max_length=100)
    priority_candidates: list[Annotated[str, Field(min_length=1, max_length=20)]] = Field(min_length=1, max_length=10)
    assignee_candidates: list[TriageAssigneeCandidate] = Field(min_length=1, max_length=100)
    top_k: int = Field(default=5, ge=1, le=20)

    @model_validator(mode="after")
    def validate_candidates(self) -> "TriageRequest":
        """候选列表 ID 和优先级不能重复。"""

        category_ids = [item.category_id for item in self.category_candidates]
        assignee_ids = [item.user_id for item in self.assignee_candidates]
        if len(set(category_ids)) != len(category_ids):
            raise ValueError("category_candidates must be unique")
        if len(set(assignee_ids)) != len(assignee_ids):
            raise ValueError("assignee_candidates must be unique")
        if len(set(self.priority_candidates)) != len(self.priority_candidates):
            raise ValueError("priority_candidates must be unique")
        return self


class TriageResponse(StrictModel):
    suggested_category_id: int | None = Field(default=None, gt=0)
    suggested_priority: str | None = Field(default=None, max_length=20)
    suggested_assignee_id: int | None = Field(default=None, gt=0)
    confidence: float = Field(ge=0, le=1)
    reason_summary: str = Field(max_length=1000)
    sources: list[SourceItem]
    degraded: bool
    reason: str | None = None


class AcceptedResponse(StrictModel):
    accepted: Literal[True] = True


class DocumentImportResponse(StrictModel):
    accepted: Literal[True] = True
    chunk_count: int = Field(gt=0)
