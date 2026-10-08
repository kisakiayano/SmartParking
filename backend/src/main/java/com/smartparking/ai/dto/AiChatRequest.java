package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 助手提问请求")
public record AiChatRequest(
        @Schema(description = "会话 ID，为空时创建新会话", example = "1001")
        Long sessionId,

        @Schema(description = "用户问题", example = "还有多少空闲车位？")
        @NotBlank(message = "question must not be blank")
        @Size(max = 500, message = "question length must not exceed 500")
        String question
) {
}
