"""Optional OpenAI-compatible chat completion client.

The service only talks to the LLM when ``OPENAI_API_KEY`` is configured. Every
failure raises :class:`LlmError` so the caller in ``main.py`` can transparently
fall back to extractive mode.
"""

from __future__ import annotations

from typing import Any, List, Optional, Sequence

import httpx

from .config import Settings, get_settings
from .retrieval import RetrievedChunk

TEMPERATURE = 0.3
MAX_TOKENS = 600
MAX_HISTORY_MESSAGES = 6
MAX_CONTEXT_CHARS = 2400


class LlmError(RuntimeError):
    """Raised when the LLM is unavailable or returns an unusable response."""


def _history_role_content(item: Any) -> tuple[str, str]:
    """Read ``role``/``content`` from a pydantic model or a plain mapping."""

    if isinstance(item, dict):
        role = item.get("role", "user")
        content = item.get("content", "")
    else:
        role = getattr(item, "role", "user")
        content = getattr(item, "content", "")
    return str(role or "user"), str(content or "")


def build_context_block(retrieved: Sequence[RetrievedChunk]) -> str:
    """Render retrieved chunks as a bounded bullet list for the prompt."""

    lines: List[str] = []
    used = 0
    for item in retrieved or []:
        text = (item.content or "").strip()
        if not text:
            continue
        line = f"- {text}"
        if used + len(line) > MAX_CONTEXT_CHARS and lines:
            break
        lines.append(line)
        used += len(line)
    return "\n".join(lines)


def build_messages(
    system_prompt: str,
    retrieved: Sequence[RetrievedChunk],
    history: Optional[Sequence[Any]],
    question: str,
) -> List[dict]:
    """Assemble the chat message list: system + context + history + question."""

    system_content = (system_prompt or "").strip()
    context = build_context_block(retrieved)
    if context:
        system_content = f"{system_content}\n\n【可参考的业务知识与实时数据】\n{context}"

    messages: List[dict] = [{"role": "system", "content": system_content}]
    for item in list(history or [])[-MAX_HISTORY_MESSAGES:]:
        role, content = _history_role_content(item)
        if role not in {"user", "assistant", "system"}:
            role = "user"
        if content.strip():
            messages.append({"role": role, "content": content})
    messages.append({"role": "user", "content": question or ""})
    return messages


async def chat_completion(
    system_prompt: str,
    retrieved: Sequence[RetrievedChunk],
    history: Optional[Sequence[Any]],
    question: str,
    settings: Optional[Settings] = None,
    client: Optional[httpx.AsyncClient] = None,
) -> str:
    """Call the OpenAI-compatible chat completion endpoint.

    Raises :class:`LlmError` on any problem (missing key, transport error,
    non-2xx status or malformed/empty payload).
    """

    settings = settings or get_settings()
    if not settings.llm_enabled:
        raise LlmError("OPENAI_API_KEY is not configured")

    url = f"{settings.openai_base_url.rstrip('/')}/chat/completions"
    payload = {
        "model": settings.openai_model,
        "messages": build_messages(system_prompt, retrieved, history, question),
        "temperature": TEMPERATURE,
        "max_tokens": MAX_TOKENS,
    }
    headers = {
        "Authorization": f"Bearer {settings.openai_api_key}",
        "Content-Type": "application/json",
    }

    try:
        if client is not None:
            response = await client.post(url, json=payload, headers=headers)
        else:
            async with httpx.AsyncClient(timeout=settings.request_timeout_seconds) as owned:
                response = await owned.post(url, json=payload, headers=headers)
    except httpx.HTTPError as exc:  # network/timeout errors
        raise LlmError(f"LLM request failed: {exc}") from exc
    except Exception as exc:  # pragma: no cover - defensive
        raise LlmError(f"LLM request failed: {exc}") from exc

    if response.status_code >= 400:
        raise LlmError(f"LLM returned HTTP {response.status_code}")

    try:
        data = response.json()
        content = data["choices"][0]["message"]["content"]
    except (ValueError, KeyError, IndexError, TypeError) as exc:
        raise LlmError("LLM response payload was not understood") from exc

    if not content or not str(content).strip():
        raise LlmError("LLM returned an empty answer")
    return str(content).strip()
