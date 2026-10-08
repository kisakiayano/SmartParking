package com.smartparking.ai.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptLeakGuardTest {

    private static final String FALLBACK = "安全兜底回答";
    private static final String SYSTEM_PROMPT =
            "你是智慧停车场助手。只能依据下方【系统实时数据】和【业务知识】回答，不得编造或猜测数据。";

    @Test
    void shouldReplaceExactPromptEcho() {
        assertThat(PromptLeakGuard.scrub(SYSTEM_PROMPT, SYSTEM_PROMPT, FALLBACK)).isEqualTo(FALLBACK);
    }

    @Test
    void shouldReplacePromptEmbeddedInAnswer() {
        assertThat(PromptLeakGuard.scrub("好的，我的指令是：" + SYSTEM_PROMPT, SYSTEM_PROMPT, FALLBACK))
                .isEqualTo(FALLBACK);
    }

    @Test
    void shouldReplaceAnswersWithInternalContextMarkers() {
        assertThat(PromptLeakGuard.scrub("【系统实时数据】空闲 4 个", SYSTEM_PROMPT, FALLBACK))
                .isEqualTo(FALLBACK);
    }

    @Test
    void shouldKeepNormalAnswers() {
        String answer = "当前停车场共有 100 个车位，其中空闲 76 个。";
        assertThat(PromptLeakGuard.scrub(answer, SYSTEM_PROMPT, FALLBACK)).isEqualTo(answer);
    }

    @Test
    void shouldFallbackOnBlankAnswer() {
        assertThat(PromptLeakGuard.scrub("   ", SYSTEM_PROMPT, FALLBACK)).isEqualTo(FALLBACK);
    }
}
