package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "用户名", example = "user01")
        @NotBlank
        @Size(min = 4, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "must contain only letters, numbers and underscores")
        String username,

        @Schema(description = "密码", example = "Password@123")
        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @Schema(description = "姓名", example = "Zhang San")
        @NotBlank
        @Size(max = 64)
        String name,

        @Schema(description = "手机号", example = "13800000000")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "must be a valid mainland China mobile number")
        String phone
) {
}
