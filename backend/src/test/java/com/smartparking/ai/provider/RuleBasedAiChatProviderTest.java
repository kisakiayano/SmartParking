package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAiChatProviderTest {

    private final AiKnowledgeBase knowledgeBase = new AiKnowledgeBase();
    private final RuleBasedAiChatProvider provider = new RuleBasedAiChatProvider(knowledgeBase);

    @Test
    void shouldAnswerWithLiveDataAndReferences() {
        AiChatContext context = new AiChatContext(
                10L,
                "user01",
                "还有多少空闲车位？",
                AiIntent.SPACE_STATUS,
                List.of("当前停车场共有 10 个车位：空闲 4 个，已预约 2 个，占用 4 个。"),
                List.of("空闲：4"),
                List.of());

        AiChatResult result = provider.chat(context);

        assertThat(result.answer()).contains("空闲 4 个").contains("在线预约");
        assertThat(result.referencedData()).containsExactly("空闲：4");
        assertThat(result.provider()).isEqualTo(RuleBasedAiChatProvider.NAME);
    }

    @Test
    void shouldAnswerGuideQuestionFromKnowledgeBase() {
        AiChatContext context = new AiChatContext(
                10L,
                "user01",
                "怎么在线预约车位？",
                AiIntent.RESERVATION_GUIDE,
                List.of(),
                List.of(),
                List.of());

        AiChatResult result = provider.chat(context);

        assertThat(result.answer()).contains("在线预约").contains("预计到达时间");
        assertThat(result.referencedData()).isEmpty();
    }

    @Test
    void shouldGreetUser() {
        AiChatContext context = new AiChatContext(
                10L,
                "user01",
                "你好",
                AiIntent.GREETING,
                List.of(),
                List.of(),
                List.of());

        AiChatResult result = provider.chat(context);

        assertThat(result.answer()).contains("智慧停车助手");
    }

    @Test
    void shouldFallbackForUnknownIntent() {
        AiChatContext context = new AiChatContext(
                10L,
                "user01",
                "今天天气怎么样",
                AiIntent.UNKNOWN,
                List.of(),
                List.of(),
                List.of());

        AiChatResult result = provider.chat(context);

        assertThat(result.answer()).contains("意见反馈");
    }
}
