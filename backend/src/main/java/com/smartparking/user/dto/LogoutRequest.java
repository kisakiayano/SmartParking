package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登出请求")
public record LogoutRequest(
        @Schema(description = "待吊销的 refresh token（可选，同时吊销 Authorization 中的 access token）")
        String refreshToken
) {
}
