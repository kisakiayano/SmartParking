"""Tests for the pure-Python BM25 lexical retrieval engine."""

from __future__ import annotations

from app import knowledge, retrieval


def test_chunks_are_loaded():
    chunks = knowledge.load_chunks()
    assert chunks, "knowledge base should not be empty"
    assert knowledge.chunk_count() == len(chunks)


def test_reservation_query_hits_reservation_chunk_in_top_three():
    results = retrieval.search("怎么在线预约车位", top_k=3)
    assert results, "expected at least one result for a known question"
    assert any("预约" in item.title or "预约" in item.content for item in results)
    assert 0.0 < results[0].score <= 1.0


def test_search_returns_at_most_top_k():
    results = retrieval.search("停车费用怎么算", top_k=2)
    assert len(results) <= 2


def test_context_snippets_have_priority():
    base = retrieval.search("怎么在线预约车位", top_k=3)
    merged = retrieval.merge_context_snippets(base, ["实时空闲车位：4 个"])
    assert merged
    assert merged[0].live is True
    assert merged[0].score == 1.0
    assert "实时空闲车位：4 个" in merged[0].content
    # Knowledge chunks are still present after the live snippet.
    assert len(merged) == len(base) + 1


def test_blank_context_snippets_are_ignored():
    base = retrieval.search("怎么在线预约车位", top_k=3)
    merged = retrieval.merge_context_snippets(base, ["   ", ""])
    assert len(merged) == len(base)


def test_unknown_gibberish_yields_empty_or_low_results():
    results = retrieval.search("zzzzqqqqvvvv 龘靐齉爩", top_k=3)
    assert results == [] or all(item.score < 0.3 for item in results)
