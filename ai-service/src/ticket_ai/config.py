"""服务配置。"""

from functools import lru_cache

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """从环境变量读取的服务配置。"""

    model_config = SettingsConfigDict(
        env_prefix="TICKET_AI_",
        extra="ignore",
    )

    service_token: str = Field(min_length=16)
    contract_version: str = "v1"
    max_request_bytes: int = Field(default=1_048_576, ge=1_024, le=10_485_760)
    elasticsearch_url: str = "http://127.0.0.1:9200"
    knowledge_index: str = "ticket-knowledge-v1"
    ticket_history_index: str = "ticket-history-v1"
    embedding_api_key: str = Field(min_length=1)
    embedding_base_url: str | None = None
    embedding_model: str = Field(min_length=1)
    llm_api_key: str | None = None
    llm_base_url: str | None = None
    llm_model: str = "gpt-4o-mini"
    llm_timeout_seconds: float = Field(default=20.0, gt=0, le=120)
    external_timeout_seconds: float = Field(default=10.0, gt=0, le=60)
    assist_rate_limit: int = Field(default=30, ge=1, le=1000)
    assist_rate_window_seconds: int = Field(default=60, ge=1, le=3600)
    smoke_mode: bool = False

    @model_validator(mode="after")
    def validate_smoke_mode(self) -> "Settings":
        """本地替身只能写入名称明确标识为 smoke/test 的索引。"""

        names = (self.knowledge_index.lower(), self.ticket_history_index.lower())
        if self.smoke_mode and not all("smoke" in name or "test" in name for name in names):
            raise ValueError("smoke mode requires smoke/test indexes")
        return self


@lru_cache
def get_settings() -> Settings:
    """返回进程级不可变配置。"""

    return Settings(_env_file=("../.env", ".env"), _env_file_encoding="utf-8")
