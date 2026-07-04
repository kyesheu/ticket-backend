"""内部 HTTP 接口认证。"""

import secrets
from typing import Annotated

from fastapi import Depends, Header, HTTPException, status

from ticket_ai.config import Settings, get_settings


def verify_service_token(
    token: Annotated[str | None, Header(alias="X-Service-Token")] = None,
    settings: Settings = Depends(get_settings),
) -> None:
    """使用常量时间比较校验服务间凭据。"""

    if token is None or not secrets.compare_digest(token, settings.service_token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid service credential")
