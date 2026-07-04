"""服务配置。"""

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """从环境变量读取的服务配置。"""

    model_config = SettingsConfigDict(env_prefix="TICKET_AI_", extra="ignore")

    service_token: str = Field(min_length=16)
    contract_version: str = "v1"
    max_request_bytes: int = Field(default=1_048_576, ge=1_024, le=10_485_760)


@lru_cache
def get_settings() -> Settings:
    """返回进程级不可变配置。"""

    return Settings()
