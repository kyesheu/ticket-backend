"""v1 内部 HTTP 接口。"""

from fastapi import APIRouter, Depends, HTTPException, Query, status

from ticket_ai.models import (
    AcceptedResponse,
    AssistRequest,
    AssistResponse,
    ClosedTicketSyncRequest,
    ClosedTicketSyncResponse,
    DocumentDetailResponse,
    DocumentImportRequest,
    DocumentImportResponse,
    DocumentOperationResponse,
    DocumentListResponse,
    HealthResponse,
    QuestionAnswerRequest,
    QuestionAnswerResponse,
    SearchResponse,
    TicketContextRequest,
    TriageRequest,
    TriageResponse,
)
from ticket_ai.security import verify_service_token
from ticket_ai.dependencies import (
    get_closed_ticket_sync_service,
    get_document_importer,
    get_knowledge_document_reader,
    get_similar_knowledge_search_service,
    get_ticket_assist_service,
    get_ticket_triage_service,
    get_health_service,
)
from ticket_ai.health import HealthService
from ticket_ai.resilience import RetrievalUnavailable, verify_ai_rate_limit
from ticket_ai.assist import TicketAssistService
from ticket_ai.triage import TicketTriageService
from ticket_ai.history_sync import ClosedTicketSyncService
from ticket_ai.knowledge import DocumentImporter, DocumentImportError, KnowledgeDocumentOperationError, KnowledgeDocumentReader
from ticket_ai.similar_search import SimilarKnowledgeSearchService

router = APIRouter(prefix="/api/v1")


@router.get("/health", response_model=HealthResponse)
def health(service: HealthService = Depends(get_health_service)) -> HealthResponse:
    """返回服务和契约版本状态。"""

    return service.check()


@router.post("/documents/import", response_model=DocumentImportResponse,
             dependencies=[Depends(verify_service_token)])
def import_document(request: DocumentImportRequest,
                    importer: DocumentImporter = Depends(get_document_importer)) -> DocumentImportResponse:
    """解析、切片、向量化并原子替换知识文档。"""

    try:
        try:
            count = importer.import_document(
                request.source_id, request.category_name or "未分类", request.file_name,
                request.content_type, request.content_base64
            )
        except TypeError:
            count = importer.import_document(
                request.source_id, request.file_name, request.content_type, request.content_base64
            )
        return DocumentImportResponse(chunk_count=count)
    except DocumentImportError as exception:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail=f"document import unavailable: {exception}") from exception


@router.get("/documents", response_model=DocumentListResponse,
            dependencies=[Depends(verify_service_token)])
def list_documents(page_num: int = 1, page_size: int = 10,
                   status_filter: str | None = Query(default=None, alias="status"),
                   category_name: str | None = None,
                   reader: KnowledgeDocumentReader = Depends(get_knowledge_document_reader)) -> DocumentListResponse:
    """分页查询知识文档运营元数据。"""

    try:
        try:
            rows, total = reader.list_documents(page_num, page_size, status_filter, category_name)
        except TypeError:
            rows, total = reader.list_documents(page_num, page_size, status_filter)
        return DocumentListResponse(rows=[_document_payload(row) for row in rows], total=total,
                                    page_num=page_num, page_size=page_size)
    except ValueError as exception:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=str(exception)) from exception


@router.get("/documents/{source_id}", response_model=DocumentDetailResponse,
            dependencies=[Depends(verify_service_token)])
def get_document(source_id: str,
                 reader: KnowledgeDocumentReader = Depends(get_knowledge_document_reader)) -> DocumentDetailResponse:
    """查询知识文档详情。"""

    document = reader.get_document(source_id)
    if document is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="document not found")
    return DocumentDetailResponse(**_document_payload(document))


def _document_payload(document: object) -> dict:
    payload = document.__dict__.copy()
    return payload


@router.post("/documents/{source_id}/reimport", response_model=DocumentOperationResponse,
             dependencies=[Depends(verify_service_token)])
def reimport_document(source_id: str,
                      reader: KnowledgeDocumentReader = Depends(get_knowledge_document_reader)) -> DocumentOperationResponse:
    """重新确认知识文档可用性并返回当前有效切片数。"""

    try:
        chunk_count = reader.reimport_document(source_id)
        return DocumentOperationResponse(chunk_count=chunk_count)
    except KnowledgeDocumentOperationError as exception:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exception)) from exception


@router.delete("/documents/{source_id}", response_model=DocumentOperationResponse,
               dependencies=[Depends(verify_service_token)])
def delete_document(source_id: str,
                    reader: KnowledgeDocumentReader = Depends(get_knowledge_document_reader)) -> DocumentOperationResponse:
    """删除知识文档的有效切片。"""

    try:
        deleted = reader.delete_document(source_id)
        return DocumentOperationResponse(chunk_count=deleted)
    except KnowledgeDocumentOperationError as exception:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=str(exception)) from exception


@router.post("/tickets/sync", response_model=ClosedTicketSyncResponse,
             dependencies=[Depends(verify_service_token)])
def sync_ticket(request: ClosedTicketSyncRequest,
                service: ClosedTicketSyncService = Depends(get_closed_ticket_sync_service)) -> ClosedTicketSyncResponse:
    """向量化并幂等写入历史已关闭工单。"""

    try:
        document = service.sync(request)
        return ClosedTicketSyncResponse(
            ticket_id=request.ticket_id,
            source_generation=document.source_generation,
        )
    except Exception as exception:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="closed ticket sync unavailable") from exception


@router.post("/knowledge/search", response_model=SearchResponse,
             dependencies=[Depends(verify_service_token), Depends(verify_ai_rate_limit)])
def search(request: TicketContextRequest,
           service: SimilarKnowledgeSearchService = Depends(get_similar_knowledge_search_service)) -> SearchResponse:
    """根据当前工单标题和描述检索知识文档与历史工单。"""

    try:
        query = f"{request.title}\n{request.description}"
        return SearchResponse(sources=[item.__dict__ for item in service.search(query)])
    except RetrievalUnavailable as exception:
        return SearchResponse(sources=[], degraded=True, reason=str(exception))
    except ValueError as exception:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=str(exception)) from exception
    except Exception:
        return SearchResponse(sources=[], degraded=True, reason="retrieval_unavailable")


@router.post("/tickets/assist", response_model=AssistResponse,
             dependencies=[Depends(verify_service_token), Depends(verify_ai_rate_limit)])
def assist(request: AssistRequest,
           service: TicketAssistService = Depends(get_ticket_assist_service)) -> AssistResponse:
    """生成仅供展示和编辑的处理建议与回复草稿。"""

    return service.assist(request)


@router.post("/qa/ask", response_model=QuestionAnswerResponse,
             dependencies=[Depends(verify_service_token), Depends(verify_ai_rate_limit)])
def ask_question(request: QuestionAnswerRequest,
                 service: TicketAssistService = Depends(get_ticket_assist_service)) -> QuestionAnswerResponse:
    """用户建单前的知识库问答。"""

    return service.answer_question(request)


@router.post("/tickets/triage", response_model=TriageResponse,
             dependencies=[Depends(verify_service_token), Depends(verify_ai_rate_limit)])
def triage(request: TriageRequest,
           service: TicketTriageService = Depends(get_ticket_triage_service)) -> TriageResponse:
    """生成受控 AI 分诊建议。"""

    return service.triage(request)
