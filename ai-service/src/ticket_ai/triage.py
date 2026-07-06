"""受控 AI 分诊服务骨架。"""

from ticket_ai.models import TriageRequest, TriageResponse


class TicketTriageService:
    """阶段49仅提供契约骨架，后续阶段实现候选排序。"""

    def triage(self, request: TriageRequest) -> TriageResponse:
        """返回不修改工单的降级分诊结果。"""

        return TriageResponse(
            suggested_category_id=None,
            suggested_priority=None,
            suggested_assignee_id=None,
            confidence=0,
            reason_summary="",
            sources=[],
            degraded=True,
            reason="stage49_contract_only",
        )
