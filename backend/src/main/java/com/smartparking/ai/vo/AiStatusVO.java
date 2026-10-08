package com.smartparking.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 提供方运行状态")
public record AiStatusVO(
        @Schema(description = "配置的提供方", example = "python")
        String configuredProvider,

        @Schema(description = "当前生效的提供方", example = "python")
        String activeProvider,

        @Schema(description = "内置规则引擎是否可用")
        boolean ruleBasedAvailable,

        @Schema(description = "OpenAI 兼容提供方是否已配置密钥")
        boolean openaiConfigured,

        @Schema(description = "Python RAG 服务是否已配置地址")
        boolean pythonConfigured,

        @Schema(description = "Python RAG 服务地址")
        String pythonBaseUrl,

        @Schema(description = "OpenAI 兼容接口地址")
        String openaiBaseUrl,

        @Schema(description = "OpenAI 兼容模型名称")
        String openaiModel
) {
}
