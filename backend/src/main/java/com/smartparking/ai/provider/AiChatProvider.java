package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;

import java.util.List;

public interface AiChatProvider {

    String provider();

    /**
     * 提供方当前是否可用，不可用时由路由自动降级到内置规则引擎。
     */
    default boolean isAvailable() {
        return true;
    }

    AiChatResult chat(AiChatContext context);
}
