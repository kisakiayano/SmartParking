"""Pydantic request/response models for the public HTTP contract.

The Java backend adapter sends JSON payloads that map onto these models. Field
names and response shapes are part of the contract and must stay stable.
"""

from __future__ import annotations

from typing import List, Literal, Optional, Union

from pydantic import BaseModel, Field, field_validator


class HealthResponse(BaseModel):
    status: str
    llm_enabled: bool
    knowledge_chunks: int


class PromptItem(BaseModel):
    code: str
    name: str
    intent: Optional[str] = None


class SearchRequest(BaseModel):
    query: str = Field(..., description="用户问题，用于业务知识检索")
    top_k: Optional[int] = Field(
        default=None, ge=1, le=50, description="返回条数，缺省使用 RAG_TOP_K"
    )


class SearchHit(BaseModel):
    doc_id: str
    title: str
    score: float
    snippet: str


class SearchResponse(BaseModel):
    results: List[SearchHit] = Field(default_factory=list)


class HistoryMessage(BaseModel):
    role: str = "user"
    content: str = ""

    @field_validator("role", "content", mode="before")
    @classmethod
    def _none_to_blank(cls, value):
        return "" if value is None else value


class ChatRequest(BaseModel):
    session_id: Optional[Union[int, str]] = None
    user_id: Optional[int] = None
    username: Optional[str] = None
    question: str = Field(..., description="用户问题")
    intent: Optional[str] = Field(default=None, description="Java 侧解析出的业务意图")
    history: List[HistoryMessage] = Field(default_factory=list)
    context_snippets: List[str] = Field(
        default_factory=list, description="Java 侧提供的实时业务数据，优先级最高"
    )
    referenced_data: List[str] = Field(default_factory=list)
    system_prompt: Optional[str] = Field(
        default=None, description="调用方覆盖的系统提示词，优先级最高"
    )

    @field_validator("history", "context_snippets", "referenced_data", mode="before")
    @classmethod
    def _none_to_empty_list(cls, value):
        # Spring/Jackson may serialize absent optional lists as JSON null.
        return [] if value is None else value


class ChatSource(BaseModel):
    title: str
    score: float
    snippet: str


class ChatResponse(BaseModel):
    answer: str
    provider: Literal["extractive", "openai"]
    referenced_data: List[str] = Field(default_factory=list)
    sources: List[ChatSource] = Field(default_factory=list)
