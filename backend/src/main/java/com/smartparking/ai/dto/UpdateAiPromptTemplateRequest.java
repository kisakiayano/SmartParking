package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "管理员更新 AI 提示词模板请求")
public record UpdateAiPromptTemplateRequest(
        @Schema(description = "模板名称", example = "通用助手提示词")
        @NotBlank(message = "templateName must not be blank")
        @Size(max = 128, message = "templateName length must not exceed 128")
        String templateName,

        @Schema(description = "系统提示词内容", example = "你是「智慧停车场管理系统」的智能客服助手……")
        @NotBlank(message = "systemPrompt must not be blank")
        @Size(max = 8000, message = "systemPrompt length must not exceed 8000")
        String systemPrompt,

        @Schema(description = "是否启用", example = "true")
        @NotNull(message = "enabled must not be null")
        Boolean enabled
) {
}
