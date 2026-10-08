"""Runtime configuration for the smart-parking AI service.

All values are read from environment variables. ``get_settings()`` builds a
fresh :class:`Settings` instance on every call so tests and operators can tweak
the environment without restarting the process or dealing with stale caches.
"""

from __future__ import annotations

import os
from dataclasses import dataclass

DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
DEFAULT_AI_SERVICE_PORT = 8000
DEFAULT_REQUEST_TIMEOUT_SECONDS = 20.0
DEFAULT_RAG_TOP_K = 4
DEFAULT_RAG_MIN_SCORE = 0.0
DEFAULT_RAG_EMBEDDING_ENABLED = False


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on", "y", "t"}


def _env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or not raw.strip():
        return default
    try:
        return int(raw.strip())
    except ValueError:
        return default


def _env_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or not raw.strip():
        return default
    try:
        return float(raw.strip())
    except ValueError:
        return default


def _env_str(name: str, default: str) -> str:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip()


@dataclass(frozen=True)
class Settings:
    """Immutable snapshot of the service configuration."""

    ai_service_port: int = DEFAULT_AI_SERVICE_PORT
    openai_base_url: str = DEFAULT_OPENAI_BASE_URL
    openai_api_key: str = ""
    openai_model: str = DEFAULT_OPENAI_MODEL
    request_timeout_seconds: float = DEFAULT_REQUEST_TIMEOUT_SECONDS
    rag_top_k: int = DEFAULT_RAG_TOP_K
    rag_min_score: float = DEFAULT_RAG_MIN_SCORE
    rag_embedding_enabled: bool = DEFAULT_RAG_EMBEDDING_ENABLED

    @property
    def llm_enabled(self) -> bool:
        """True when an OpenAI-compatible API key is configured."""

        return bool(self.openai_api_key and self.openai_api_key.strip())


def get_settings() -> Settings:
    """Read the current environment into a :class:`Settings` instance."""

    return Settings(
        ai_service_port=_env_int("AI_SERVICE_PORT", DEFAULT_AI_SERVICE_PORT),
        openai_base_url=_env_str("OPENAI_BASE_URL", DEFAULT_OPENAI_BASE_URL).rstrip("/")
        or DEFAULT_OPENAI_BASE_URL,
        openai_api_key=_env_str("OPENAI_API_KEY", ""),
        openai_model=_env_str("OPENAI_MODEL", DEFAULT_OPENAI_MODEL) or DEFAULT_OPENAI_MODEL,
        request_timeout_seconds=_env_float(
            "REQUEST_TIMEOUT_SECONDS", DEFAULT_REQUEST_TIMEOUT_SECONDS
        ),
        rag_top_k=max(1, _env_int("RAG_TOP_K", DEFAULT_RAG_TOP_K)),
        rag_min_score=_env_float("RAG_MIN_SCORE", DEFAULT_RAG_MIN_SCORE),
        # Reserved for a future embedding-based retrieval path. Lexical BM25 is
        # the default and only active retrieval strategy today.
        rag_embedding_enabled=_env_bool(
            "RAG_EMBEDDING_ENABLED", DEFAULT_RAG_EMBEDDING_ENABLED
        ),
    )
