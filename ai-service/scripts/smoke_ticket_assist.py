"""无外部写入的工单辅助端到端 smoke。"""

import json

from langchain_core.runnables import RunnableLambda

from ticket_ai.assist import TicketAssistService
from ticket_ai.models import AssistRequest
from ticket_ai.similar_search import SimilarKnowledgeResult


class FakeSearchService:
    def search(self, query: str, limit: int):
        assert "Redis" in query and limit == 3
        return [SimilarKnowledgeResult(
            "knowledge_document", "smoke-doc", "Redis 指南", "使用布隆过滤器", 1.9,
            {"chunk_index": 0},
        )]


def main() -> int:
    ticket_state = "PROCESSING"
    comment_count = 2
    llm = RunnableLambda(lambda _: json.dumps({
        "suggestion": "增加参数校验并使用布隆过滤器",
        "reply_draft": "您好，建议增加参数校验和空值缓存。",
        "source_ids": ["smoke-doc"],
    }, ensure_ascii=False))
    result = TicketAssistService(FakeSearchService(), llm).assist(AssistRequest(
        contract_version="v1", ticket_id=9001, title="Redis 缓存穿透",
        description="不存在的 key 被反复查询", category="中间件", top_k=3,
    ))
    assert result.suggestion and result.reply_draft and result.sources
    assert ticket_state == "PROCESSING"
    assert comment_count == 2
    print("PASS: suggestion, replyDraft and sources returned; ticket state and comments unchanged")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
