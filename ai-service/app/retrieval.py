"""Pure-Python BM25-style lexical retrieval.

No numpy, no vector database and no third-party search dependency: the corpus is
tiny (three markdown files), so an in-memory inverted index is more than enough.

Tokenization mixes:
* single CJK characters and CJK character bigrams, which gives useful Chinese
  matching without a word segmenter;
* lowercased ASCII words.
"""

from __future__ import annotations

import math
import re
from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
from typing import Dict, List, Optional, Sequence, Tuple

from .config import get_settings
from .knowledge import KnowledgeChunk, load_chunks

#: BM25 free parameters.
K1 = 1.5
B = 0.75

_SNIPPET_LENGTH = 160
_ASCII_WORD_RE = re.compile(r"[a-z0-9]+")
_CJK_RUN_RE = re.compile(r"[\u4e00-\u9fff]+")


@dataclass(frozen=True)
class RetrievedChunk:
    """A single retrieval result.

    ``live`` marks chunks that came from Java-provided real-time data rather
    than the static knowledge base; those always score ``1.0`` so live data
    wins over documentation.
    """

    doc_id: str
    title: str
    content: str
    score: float
    snippet: str
    live: bool = False


def tokenize(text: str) -> List[str]:
    """Tokenize mixed Chinese/ASCII text into BM25 terms."""

    if not text:
        return []
    lowered = text.lower()
    tokens: List[str] = _ASCII_WORD_RE.findall(lowered)
    for run in _CJK_RUN_RE.findall(text):
        tokens.extend(run)
        tokens.extend(run[i : i + 2] for i in range(len(run) - 1))
    return tokens


def make_snippet(text: str, limit: int = _SNIPPET_LENGTH) -> str:
    """Collapse whitespace and truncate to a readable snippet."""

    cleaned = re.sub(r"\s+", " ", text or "").strip()
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[:limit].rstrip() + "…"


class BM25Index:
    """In-memory BM25 index built from a fixed list of chunks."""

    def __init__(self, chunks: Sequence[KnowledgeChunk]) -> None:
        self.chunks: List[KnowledgeChunk] = list(chunks)
        self.term_freqs: List[Counter] = [
            Counter(tokenize(f"{chunk.title} {chunk.content}")) for chunk in self.chunks
        ]
        self.doc_lengths: List[int] = [sum(tf.values()) for tf in self.term_freqs]
        total_length = sum(self.doc_lengths)
        self.avgdl = total_length / len(self.chunks) if self.chunks else 0.0

        document_frequency: Counter = Counter()
        for tf in self.term_freqs:
            document_frequency.update(tf.keys())

        count = len(self.chunks)
        self.idf: Dict[str, float] = {
            term: math.log(1.0 + (count - df + 0.5) / (df + 0.5))
            for term, df in document_frequency.items()
        }

    def score(self, query: str) -> List[Tuple[int, float]]:
        """Return ``(chunk_index, raw_bm25_score)`` sorted by score desc."""

        terms = set(tokenize(query))
        if not terms or not self.chunks:
            return []

        scored: List[Tuple[int, float]] = []
        for index, term_freq in enumerate(self.term_freqs):
            doc_length = self.doc_lengths[index] or 1
            denominator_length = 1 - B + B * doc_length / (self.avgdl or 1.0)
            score = 0.0
            for term in terms:
                frequency = term_freq.get(term, 0)
                if not frequency:
                    continue
                idf = self.idf.get(term, 0.0)
                if idf <= 0:
                    continue
                score += idf * (frequency * (K1 + 1)) / (
                    frequency + K1 * denominator_length
                )
            if score > 0:
                scored.append((index, score))

        scored.sort(key=lambda item: item[1], reverse=True)
        return scored


@lru_cache(maxsize=1)
def get_index() -> BM25Index:
    """Build the index once and reuse it for the process lifetime."""

    return BM25Index(load_chunks())


def _normalize(raw_score: float) -> float:
    """Map an unbounded BM25 score into ``[0, 1)`` monotonically."""

    if raw_score <= 0:
        return 0.0
    return round(min(raw_score / (raw_score + 1.0), 1.0), 4)


def search(query: str, top_k: Optional[int] = None) -> List[RetrievedChunk]:
    """Retrieve the best matching knowledge chunks for ``query``."""

    settings = get_settings()
    limit = top_k if top_k and top_k > 0 else settings.rag_top_k
    scored = get_index().score(query or "")

    results: List[RetrievedChunk] = []
    for index, raw_score in scored:
        normalized = _normalize(raw_score)
        if normalized < settings.rag_min_score:
            continue
        chunk = get_index().chunks[index]
        results.append(
            RetrievedChunk(
                doc_id=chunk.doc_id,
                title=chunk.title,
                content=chunk.content,
                score=normalized,
                snippet=make_snippet(chunk.content),
            )
        )
        if len(results) >= limit:
            break
    return results


def merge_context_snippets(
    chunks: Sequence[RetrievedChunk], context_snippets: Optional[Sequence[str]]
) -> List[RetrievedChunk]:
    """Prepend live Java-provided snippets ahead of knowledge chunks.

    Live snippets are assigned ``score = 1.0`` (the maximum possible score) so
    real-time data always outranks static documentation.
    """

    merged: List[RetrievedChunk] = []
    for raw in context_snippets or []:
        text = (raw or "").strip()
        if not text:
            continue
        merged.append(
            RetrievedChunk(
                doc_id="live-context",
                title="实时数据",
                content=text,
                score=1.0,
                snippet=make_snippet(text),
                live=True,
            )
        )
    merged.extend(chunks or [])
    return merged
