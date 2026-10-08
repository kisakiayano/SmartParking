package com.smartparking.ai.provider;

import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由降级保证：外部提供方失败或不可用时必须回退到内置规则引擎。
 */
class AiChatProviderRouterTest {

    private static final String PYTHON = PythonAiChatProvider.NAME;

    @Test
    void shouldFallbackToRuleBasedWhenPrimaryThrows() {
        AiChatProvider primary = provider(PYTHON, true, () -> {
            throw new AiProviderException("boom");
        });
        AiChatProvider ruleBased = ruleBasedAnswer("rule answer");
        AiChatProviderRouter router = new AiChatProviderRouter(List.of(primary, ruleBased), properties(PYTHON));

        AiChatResult result = router.chat(context());

        assertThat(result.answer()).isEqualTo("rule answer");
        assertThat(result.provider()).isEqualTo(RuleBasedAiChatProvider.NAME);
    }

    @Test
    void shouldUsePrimaryResultWhenPrimarySucceeds() {
        AiChatProvider primary = provider(PYTHON, true,
                () -> new AiChatResult("python answer", List.of(), PYTHON));
        AiChatProvider ruleBased = ruleBasedAnswer("rule answer");
        AiChatProviderRouter router = new AiChatProviderRouter(List.of(primary, ruleBased), properties(PYTHON));

        AiChatResult result = router.chat(context());

        assertThat(result.answer()).isEqualTo("python answer");
        assertThat(result.provider()).isEqualTo(PYTHON);
    }

    @Test
    void shouldUseRuleBasedWhenConfiguredPrimaryUnavailable() {
        AiChatProvider primary = provider(PYTHON, false,
                () -> new AiChatResult("python answer", List.of(), PYTHON));
        AiChatProvider ruleBased = ruleBasedAnswer("rule answer");
        AiChatProviderRouter router = new AiChatProviderRouter(List.of(primary, ruleBased), properties(PYTHON));

        AiChatResult result = router.chat(context());

        assertThat(result.answer()).isEqualTo("rule answer");
        assertThat(result.provider()).isEqualTo(RuleBasedAiChatProvider.NAME);
    }

    @Test
    void shouldReportRuleBasedAsActiveProviderWhenConfiguredProviderUnknown() {
        AiChatProvider primary = provider(PYTHON, true,
                () -> new AiChatResult("python answer", List.of(), PYTHON));
        AiChatProvider ruleBased = ruleBasedAnswer("rule answer");
        AiChatProviderRouter router = new AiChatProviderRouter(List.of(primary, ruleBased), properties("missing"));

        assertThat(router.activeProvider()).isEqualTo(RuleBasedAiChatProvider.NAME);
    }

    private AiChatProvider ruleBasedAnswer(String answer) {
        return provider(RuleBasedAiChatProvider.NAME, true,
                () -> new AiChatResult(answer, List.of(), RuleBasedAiChatProvider.NAME));
    }

    private AiChatProvider provider(String name, boolean available, Supplier<AiChatResult> chat) {
        return new AiChatProvider() {
            @Override
            public String provider() {
                return name;
            }

            @Override
            public boolean isAvailable() {
                return available;
            }

            @Override
            public AiChatResult chat(AiChatContext context) {
                return chat.get();
            }
        };
    }

    private AiProperties properties(String provider) {
        AiProperties properties = new AiProperties();
        properties.setProvider(provider);
        return properties;
    }

    private AiChatContext context() {
        return new AiChatContext(
                10L,
                "user01",
                "还有多少空闲车位？",
                AiIntent.SPACE_STATUS,
                List.of(),
                List.of(),
                List.of());
    }
}
