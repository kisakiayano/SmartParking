package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新 Token 请求")
public record RefreshTokenRequest(
        @Schema(description = "当前有效 JWT")
        @NotBlank
        String token
) {
}
