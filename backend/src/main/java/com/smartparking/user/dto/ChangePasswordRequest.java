package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @Schema(description = "旧密码")
        @NotBlank
        String oldPassword,

        @Schema(description = "新密码")
        @NotBlank
        @Size(min = 8, max = 64)
        String newPassword
) {
}
