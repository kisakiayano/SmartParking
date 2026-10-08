package com.smartparking.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginVO(
        @Schema(description = "访问令牌（access token）")
        String token,

        @Schema(description = "刷新令牌（refresh token），用于 /auth/refresh 轮换")
        String refreshToken,

        @Schema(description = "当前用户")
        UserVO user
) {
}
