package com.smartparking.ai.provider;

import java.util.List;

public record AiChatResult(String answer, List<String> referencedData, String provider) {
}
