package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置规则引擎实现：直接使用实时业务数据和内置知识库作答，无需外部大模型。
 */
@Component
public class RuleBasedAiChatProvider implements AiChatProvider {

    public static final String NAME = "rule-based";

    private final AiKnowledgeBase knowledgeBase;

    public RuleBasedAiChatProvider(AiKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public String provider() {
        return NAME;
    }

    @Override
    public AiChatResult chat(AiChatContext context) {
        if (context.intent() == AiIntent.GREETING) {
            return new AiChatResult(knowledgeBase.greetingAnswer(), List.of(), NAME);
        }
        if (context.intent() != AiIntent.UNKNOWN && context.knowledgeSnippets() != null
                && !context.knowledgeSnippets().isEmpty()) {
            String answer = String.join("\n", context.knowledgeSnippets());
            String hint = knowledgeBase.followUpHint(context.intent());
            if (hint != null) {
                answer = answer + "\n" + hint;
            }
            return new AiChatResult(answer, context.referencedData(), NAME);
        }
        String answer = knowledgeBase.answerFor(context.intent());
        if (answer == null) {
            answer = knowledgeBase.fallbackAnswer();
        }
        return new AiChatResult(answer, List.of(), NAME);
    }
}
