package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;

import java.util.List;

public record AiChatContext(
        Long userId,
        String username,
        String question,
        AiIntent intent,
        List<String> knowledgeSnippets,
        List<String> referencedData,
        List<AiHistoryMessage> history
) {
}
