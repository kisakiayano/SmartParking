"""FastAPI application for the smart-parking AI (RAG) service.

Endpoints (see ``README.md`` for the full contract):
* ``GET  /health``
* ``GET  /v1/prompts``
* ``POST /v1/rag/search``
* ``POST /v1/chat``

The service is called server-to-server by the Java backend, so CORS is not
needed. When no ``OPENAI_API_KEY`` is configured the chat endpoint always
answers in extractive mode and never fails for ordinary unknown questions.
"""

from __future__ import annotations

import logging
from typing import List, Sequence

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from . import config, knowledge, llm, prompts, retrieval
from .models import (
    ChatRequest,
    ChatResponse,
    ChatSource,
    HealthResponse,
    PromptItem,
    SearchHit,
    SearchRequest,
    SearchResponse,
)
from .retrieval import RetrievedChunk

logger = logging.getLogger("ai-service")

app = FastAPI(
    title="Smart Parking AI Service",
    version="1.0.0",
    description="RAG service over parking-domain knowledge with preset prompts.",
)


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Return structured JSON instead of a bare stack trace."""

    logger.exception("Unhandled error while processing %s", request.url.path)
    return JSONResponse(status_code=500, content={"detail": "AI service internal error"})


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    settings = config.get_settings()
    return HealthResponse(
        status="ok",
        llm_enabled=settings.llm_enabled,
        knowledge_chunks=knowledge.chunk_count(),
    )


@app.get("/v1/prompts", response_model=List[PromptItem])
def list_prompts() -> List[PromptItem]:
    return [
        PromptItem(code=preset.code, name=preset.name, intent=preset.intent)
        for preset in prompts.list_prompts()
    ]


@app.post("/v1/rag/search", response_model=SearchResponse)
def rag_search(payload: SearchRequest) -> SearchResponse:
    results = retrieval.search(payload.query, payload.top_k)
    return SearchResponse(
        results=[
            SearchHit(
                doc_id=item.doc_id,
                title=item.title,
                score=item.score,
                snippet=item.snippet,
            )
            for item in results
        ]
    )


def _build_extractive_answer(
    merged: Sequence[RetrievedChunk], referenced_data: Sequence[str]
) -> str:
    """Compose a readable Chinese answer from the best retrieved chunks."""

    live = [item for item in merged if item.live]
    documented = [item for item in merged if not item.live]

    parts: List[str] = []
    if live:
        live_text = "；".join(item.content.strip() for item in live[:3] if item.content.strip())
        if live_text:
            parts.append(f"根据当前实时数据：{live_text}。")

    if documented:
        primary = documented[0].content.strip()
        if primary:
            parts.append(primary)
        if len(documented) > 1 and len(primary) < knowledge.MIN_CHUNK_CHARS:
            secondary = documented[1].content.strip()
            if secondary and secondary not in primary:
                parts.append(secondary)

    if not parts:
        return prompts.fallback_answer()

    if referenced_data:
        values = [str(item) for item in referenced_data if str(item).strip()]
        if values:
            parts.append("参考数据：" + "；".join(values))
    return "\n".join(part for part in parts if part)


@app.post("/v1/chat", response_model=ChatResponse)
async def chat(payload: ChatRequest) -> ChatResponse:
    settings = config.get_settings()

    # Greetings are deterministic and must not depend on retrieval or the LLM.
    # Never echo the resolved system prompt back as the answer (prompt leak).
    if (payload.intent or "").strip().upper() == prompts.GREETING_PROMPT_CODE:
        return ChatResponse(
            answer=prompts.greeting_answer(),
            provider="extractive",
            referenced_data=list(payload.referenced_data),
            sources=[],
        )

    system_prompt = prompts.resolve_prompt(payload.intent, payload.system_prompt)

    knowledge_hits = retrieval.search(payload.question, settings.rag_top_k)
    merged = retrieval.merge_context_snippets(knowledge_hits, payload.context_snippets)

    provider = "extractive"
    answer = ""
    if settings.llm_enabled:
        try:
            answer = await llm.chat_completion(
                system_prompt,
                merged,
                payload.history,
                payload.question,
                settings=settings,
            )
            provider = "openai"
        except llm.LlmError as exc:
            logger.warning("LLM call failed, using extractive fallback: %s", exc)
            answer = ""

    if not answer:
        provider = "extractive"
        answer = _build_extractive_answer(merged, payload.referenced_data)

    # 安全兜底：任何路径的回答如果复述了系统提示词或内部上下文，一律替换为安全回答。
    answer = prompts.scrub_prompt_leak(answer, system_prompt)

    sources = [
        ChatSource(title=item.title, score=item.score, snippet=item.snippet)
        for item in merged
        if item.snippet
    ]
    return ChatResponse(
        answer=answer,
        provider=provider,
        referenced_data=list(payload.referenced_data),
        sources=sources,
    )


if __name__ == "__main__":
    _settings = config.get_settings()
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=_settings.ai_service_port,
        reload=False,
    )
