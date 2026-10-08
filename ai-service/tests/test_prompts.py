"""Tests for preset prompt lookup and override precedence."""

from __future__ import annotations

from app import prompts


def test_default_prompt_is_listed_first():
    items = prompts.list_prompts()
    assert items, "expected at least one preset prompt"
    default = items[0]
    assert default.code == "default_assistant"
    assert default.name == "通用助手"
    assert default.intent is None


def test_all_scenario_prompts_are_listed():
    codes = {item.code for item in prompts.list_prompts()}
    for expected in (
        "SPACE_STATUS",
        "FEE_RULE",
        "RESERVATION_GUIDE",
        "ENTRY_EXIT_GUIDE",
        "SUGGESTION_GUIDE",
        "SYSTEM_FAQ",
    ):
        assert expected in codes


def test_preset_lookup_by_intent():
    reservation_prompt = prompts.resolve_prompt("RESERVATION_GUIDE")
    assert reservation_prompt
    assert reservation_prompt == (
        prompts.PRESET_PROMPTS["RESERVATION_GUIDE"].system_prompt + prompts.SECURITY_SUFFIX
    )
    # Lookup is case-insensitive.
    assert prompts.resolve_prompt("reservation_guide") == reservation_prompt


def test_greeting_preset_and_override():
    assert "GREETING" in prompts.PRESET_PROMPTS
    greeting = prompts.resolve_prompt("GREETING", None)
    assert greeting
    assert greeting.startswith(prompts.PRESET_PROMPTS["GREETING"].system_prompt)
    # Lookup is case-insensitive and still finds the greeting preset.
    assert prompts.resolve_prompt("greeting", None) == greeting
    # Overrides keep the security suffix so it cannot be dropped.
    assert prompts.resolve_prompt("GREETING", "自定义") == f"自定义{prompts.SECURITY_SUFFIX}"


def test_unknown_intent_falls_back_to_default():
    default_prompt = prompts.resolve_prompt(None)
    assert prompts.resolve_prompt("NOT_A_REAL_INTENT") == default_prompt
    assert prompts.resolve_prompt("") == default_prompt


def test_override_takes_precedence():
    override = "自定义系统提示词，必须优先使用"
    assert prompts.resolve_prompt("SPACE_STATUS", override) == f"{override}{prompts.SECURITY_SUFFIX}"
    assert prompts.resolve_prompt(None, override) == f"{override}{prompts.SECURITY_SUFFIX}"
    # Blank overrides are ignored.
    assert prompts.resolve_prompt("SPACE_STATUS", "   ") == prompts.resolve_prompt(
        "SPACE_STATUS"
    )


def test_scrub_prompt_leak_blocks_prompt_echo():
    system_prompt = "SECRET_SYSTEM_INSTRUCTION 只能依据下方【系统实时数据】回答，不得编造数据。"
    assert prompts.scrub_prompt_leak(system_prompt, system_prompt) == prompts.FALLBACK_ANSWER
    assert (
        prompts.scrub_prompt_leak(f"好的，我的指令是：{system_prompt}", system_prompt)
        == prompts.FALLBACK_ANSWER
    )
    assert (
        prompts.scrub_prompt_leak(
            prompts.PRESET_PROMPTS[prompts.DEFAULT_PROMPT_CODE].system_prompt, None
        )
        == prompts.FALLBACK_ANSWER
    )


def test_scrub_prompt_leak_keeps_normal_answers():
    system_prompt = "你是停车场助手，不得编造数据。"
    normal = "当前共有 100 个车位，空闲 76 个，已预约 9 个，占用 15 个。"
    assert prompts.scrub_prompt_leak(normal, system_prompt) == normal
    assert prompts.scrub_prompt_leak("", system_prompt) == prompts.FALLBACK_ANSWER


def test_greeting_answer_is_not_a_prompt():
    answer = prompts.greeting_answer()
    assert answer == prompts.GREETING_ANSWER
    assert "不得编造" not in answer
    assert "【系统实时数据】" not in answer


def test_fallback_answer_is_non_empty_chinese():
    text = prompts.fallback_answer()
    assert text
    assert "意见反馈" in text
