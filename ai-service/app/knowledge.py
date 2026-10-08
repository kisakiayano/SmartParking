"""Load and chunk the parking-domain markdown knowledge base.

Chunking strategy: split each markdown file by headings, then group the
paragraphs inside each section into chunks of roughly 120-400 characters while
keeping the document id and a human-readable section title for every chunk.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import List, Tuple

KNOWLEDGE_DIR = Path(__file__).resolve().parent.parent / "data" / "knowledge"

_HEADING_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*$")
MIN_CHUNK_CHARS = 120
MAX_CHUNK_CHARS = 400


@dataclass(frozen=True)
class KnowledgeChunk:
    doc_id: str
    title: str
    content: str


def _split_sections(markdown: str, fallback_title: str) -> List[Tuple[str, str]]:
    """Split markdown into ``(section_title, body)`` pairs by headings."""

    sections: List[Tuple[str, str]] = []
    doc_title = fallback_title
    current_title = fallback_title
    buffer: List[str] = []

    def flush() -> None:
        body = "\n".join(buffer).strip()
        if body:
            sections.append((current_title, body))
        buffer.clear()

    for line in markdown.splitlines():
        match = _HEADING_RE.match(line)
        if match:
            flush()
            level = len(match.group(1))
            heading = match.group(2).strip()
            if level == 1:
                doc_title = heading or fallback_title
                current_title = doc_title
            else:
                current_title = f"{doc_title} - {heading}" if doc_title else heading
        else:
            buffer.append(line)

    flush()
    return sections


def _split_paragraphs(body: str) -> List[str]:
    parts = re.split(r"\n\s*\n", body)
    return [part.strip() for part in parts if part.strip()]


def _hard_wrap(text: str, max_chars: int) -> List[str]:
    """Split an oversized paragraph near sentence boundaries."""

    pieces: List[str] = []
    remaining = text.strip()
    while len(remaining) > max_chars:
        cut = remaining.rfind("。", 0, max_chars)
        if cut < max_chars // 2:
            cut = remaining.rfind("；", 0, max_chars)
        if cut < max_chars // 2:
            cut = remaining.rfind("\n", 0, max_chars)
        if cut < max_chars // 2:
            cut = max_chars - 1
        pieces.append(remaining[: cut + 1].strip())
        remaining = remaining[cut + 1 :].strip()
    if remaining:
        pieces.append(remaining)
    return pieces


def _group_paragraphs(
    paragraphs: List[str],
    min_chars: int = MIN_CHUNK_CHARS,
    max_chars: int = MAX_CHUNK_CHARS,
) -> List[str]:
    """Group paragraphs into chunks of roughly 120-400 characters."""

    chunks: List[str] = []
    current = ""
    for paragraph in paragraphs:
        for piece in _hard_wrap(paragraph, max_chars):
            if not current:
                current = piece
                continue
            candidate = f"{current}\n{piece}"
            if len(candidate) <= max_chars:
                current = candidate
            else:
                chunks.append(current)
                current = piece

    if current:
        if (
            chunks
            and len(current) < min_chars
            and len(chunks[-1]) + len(current) + 1 <= max_chars
        ):
            chunks[-1] = f"{chunks[-1]}\n{current}"
        else:
            chunks.append(current)
    return chunks


def _load_file(path: Path) -> List[KnowledgeChunk]:
    text = path.read_text(encoding="utf-8")
    doc_id = path.stem
    chunks: List[KnowledgeChunk] = []
    for section_title, body in _split_sections(text, doc_id):
        grouped = _group_paragraphs(_split_paragraphs(body))
        for index, group in enumerate(grouped):
            title = section_title if index == 0 else f"{section_title}（{index + 1}）"
            chunks.append(KnowledgeChunk(doc_id=doc_id, title=title, content=group))
    return chunks


@lru_cache(maxsize=1)
def load_chunks() -> Tuple[KnowledgeChunk, ...]:
    """Load every ``*.md`` file in the knowledge directory once and cache it."""

    if not KNOWLEDGE_DIR.is_dir():
        return tuple()
    chunks: List[KnowledgeChunk] = []
    for path in sorted(KNOWLEDGE_DIR.glob("*.md")):
        chunks.extend(_load_file(path))
    return tuple(chunks)


def chunk_count() -> int:
    """Number of knowledge chunks, used by ``GET /health``."""

    return len(load_chunks())


def get_knowledge_dir() -> Path:
    return KNOWLEDGE_DIR
