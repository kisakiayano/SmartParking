package com.smartparking.ai.exception;

/**
 * AI 提供方调用失败时抛出，由路由组件决定是否降级到内置规则引擎。
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
