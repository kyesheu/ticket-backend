"""v1 内部 HTTP 接口。"""

from fastapi import APIRouter, Depends, HTTPException, status

from ticket_ai.models import (
    AcceptedResponse,
    AssistResponse,
    ClosedTicketSyncRequest,
    DocumentImportRequest,
    DocumentImportResponse,
    HealthResponse,
    SearchResponse,
    TicketContextRequest,
)
from ticket_ai.security import verify_service_token
from ticket_ai.dependencies import get_document_importer
from ticket_ai.knowledge import DocumentImporter, DocumentImportError

router = APIRouter(prefix="/api/v1")


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """返回服务和契约版本状态。"""

    return HealthResponse()


@router.post("/documents/import", response_model=DocumentImportResponse,
             dependencies=[Depends(verify_service_token)])
def import_document(request: DocumentImportRequest,
                    importer: DocumentImporter = Depends(get_document_importer)) -> DocumentImportResponse:
    """解析、切片、向量化并原子替换知识文档。"""

    try:
        count = importer.import_document(
            request.source_id, request.file_name, request.content_type, request.content_base64
        )
        return DocumentImportResponse(chunk_count=count)
    except DocumentImportError as exception:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_CONTENT, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="document import unavailable") from exception


@router.post("/tickets/sync", response_model=AcceptedResponse, dependencies=[Depends(verify_service_token)])
def sync_ticket(request: ClosedTicketSyncRequest) -> AcceptedResponse:
    """接收关闭工单快照；阶段四十六实现实际同步。"""

    del request
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="stage 46 not implemented")


@router.post("/knowledge/search", response_model=SearchResponse, dependencies=[Depends(verify_service_token)])
def search(request: TicketContextRequest) -> SearchResponse:
    """检索相似知识；阶段四十六实现实际检索。"""

    del request
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="stage 46 not implemented")


@router.post("/tickets/assist", response_model=AssistResponse, dependencies=[Depends(verify_service_token)])
def assist(request: TicketContextRequest) -> AssistResponse:
    """生成处理建议和回复草稿；阶段四十七实现。"""

    del request
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="stage 47 not implemented")
