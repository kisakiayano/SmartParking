package com.smartparking.ai.support;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统提示词泄露防护：当模型输出复述了系统提示词或内部上下文时，替换为安全回答。
 */
public final class PromptLeakGuard {

    private static final List<String> LEAK_MARKERS = List.of(
            "【系统实时数据】",
            "【业务知识】",
            "【可参考的业务知识与实时数据】",
            "只能依据下方",
            "不得编造或猜测数据"
    );

    private static final int MIN_MATCH_LENGTH = 40;

    private PromptLeakGuard() {
    }

    public static String scrub(String answer, String systemPrompt, String safeFallback) {
        if (!StringUtils.hasText(answer)) {
            return safeFallback;
        }
        String normalizedAnswer = normalize(answer);
        if (!StringUtils.hasText(normalizedAnswer)) {
            return safeFallback;
        }
        if (LEAK_MARKERS.stream().anyMatch(normalizedAnswer::contains)) {
            return safeFallback;
        }

        String normalizedPrompt = normalize(systemPrompt);
        if (!StringUtils.hasText(normalizedPrompt)) {
            return answer;
        }
        if (normalizedAnswer.equals(normalizedPrompt)) {
            return safeFallback;
        }
        if (normalizedPrompt.length() >= MIN_MATCH_LENGTH && normalizedAnswer.contains(normalizedPrompt)) {
            return safeFallback;
        }
        if (normalizedAnswer.length() >= MIN_MATCH_LENGTH
                && normalizedPrompt.contains(normalizedAnswer)
                && normalizedAnswer.length() >= normalizedPrompt.length() / 2) {
            return safeFallback;
        }
        return answer;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }
}
