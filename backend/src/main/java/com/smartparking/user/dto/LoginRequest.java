package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "用户名", example = "user01")
        @NotBlank
        String username,

        @Schema(description = "密码", example = "Password@123")
        @NotBlank
        String password
) {
}
