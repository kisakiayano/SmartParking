package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "修改用户状态请求")
public record UpdateUserStatusRequest(
        @Schema(description = "用户状态", example = "ENABLED")
        @NotBlank
        @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
        String status
) {
}
