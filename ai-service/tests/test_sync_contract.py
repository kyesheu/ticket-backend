"""历史工单同步 v1 契约测试。"""

from datetime import datetime, timezone

import pytest
from pydantic import ValidationError

from ticket_ai.models import ClosedTicketSyncRequest, ClosedTicketSyncResponse


def valid_payload() -> dict:
    return {
        "contract_version": "v1",
        "ticket_id": 42,
        "title": "Redis 缓存穿透",
        "category": "中间件",
        "description": "不存在的 key 被反复查询",
        "solution": "参数校验、空值缓存和布隆过滤器",
        "status": "CLOSED",
        "tags": ["Redis", "缓存"],
        "created_time": "2026-07-01T09:00:00+08:00",
        "closed_time": "2026-07-01T10:00:00+08:00",
        "source_generation": 3,
    }


def test_accepts_complete_closed_ticket_contract() -> None:
    request = ClosedTicketSyncRequest.model_validate(valid_payload())

    assert request.ticket_id == 42
    assert request.created_time.tzinfo is not None
    assert ClosedTicketSyncResponse(ticket_id=42, source_generation=3).model_dump() == {
        "accepted": True, "ticket_id": 42, "source_generation": 3
    }


@pytest.mark.parametrize(
    ("field", "value"),
    [("ticket_id", 0), ("title", ""), ("category", ""), ("description", ""),
     ("solution", ""), ("status", "PROCESSING"), ("tags", ["x"] * 21),
     ("source_generation", 0)],
)
def test_rejects_invalid_basic_fields(field: str, value: object) -> None:
    payload = valid_payload()
    payload[field] = value

    with pytest.raises(ValidationError):
        ClosedTicketSyncRequest.model_validate(payload)


def test_rejects_naive_or_reversed_times_and_duplicate_tags() -> None:
    for updates in (
        {"created_time": datetime(2026, 7, 1, 9), "closed_time": datetime(2026, 7, 1, 10)},
        {"created_time": datetime(2026, 7, 1, 10, tzinfo=timezone.utc),
         "closed_time": datetime(2026, 7, 1, 9, tzinfo=timezone.utc)},
        {"tags": ["Redis", "Redis"]},
    ):
        payload = valid_payload()
        payload.update(updates)
        with pytest.raises(ValidationError):
            ClosedTicketSyncRequest.model_validate(payload)
