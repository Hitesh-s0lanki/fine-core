"""Validated service configuration loaded at application startup."""

from pathlib import Path
from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="FINECORE_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = Field(default="finecore-ai-server", min_length=1)
    environment: Literal["development", "test", "production"] = "development"

    adapter_registry_dir: Path = Path(__file__).resolve().parents[3] / "temp" / "adapter-registry"

    core_api_url: str = "http://127.0.0.1:8080"
    ingestion_allowed_origins: list[str] = Field(default_factory=list)
    ingestion_raw_dir: Path = Path(__file__).resolve().parents[3] / "temp" / "ingestion-raw"
    ingestion_user_agent: str = Field(default="FinCore-local-ingestion/0.1", min_length=1)
    ingestion_max_bytes: int = Field(default=5_000_000, ge=1024, le=50_000_000)
    ingestion_poll_seconds: float = Field(default=5, ge=1, le=60)
