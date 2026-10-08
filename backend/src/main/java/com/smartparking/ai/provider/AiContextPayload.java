package com.smartparking.ai.provider;

import java.util.List;

public record AiContextPayload(List<String> answers, List<String> referencedData) {

    public static AiContextPayload empty() {
        return new AiContextPayload(List.of(), List.of());
    }
}
