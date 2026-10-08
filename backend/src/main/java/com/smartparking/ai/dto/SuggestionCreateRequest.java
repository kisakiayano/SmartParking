package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "提交用户建议请求")
public record SuggestionCreateRequest(
        @Schema(description = "建议分类", example = "FUNCTION")
        @NotBlank(message = "category must not be blank")
        @Pattern(regexp = "^(FUNCTION|EXPERIENCE|BUG|CHARGE|OTHER)$",
                message = "category must be FUNCTION, EXPERIENCE, BUG, CHARGE or OTHER")
        String category,

        @Schema(description = "标题", example = "希望支持微信支付")
        @NotBlank(message = "title must not be blank")
        @Size(max = 128, message = "title length must not exceed 128")
        String title,

        @Schema(description = "建议内容", example = "出场缴费时希望增加微信扫码支付。")
        @NotBlank(message = "content must not be blank")
        @Size(max = 2000, message = "content length must not exceed 2000")
        String content,

        @Schema(description = "联系方式，可选", example = "13800000000")
        @Size(max = 64, message = "contact length must not exceed 64")
        String contact
) {
}
