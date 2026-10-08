package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "修改个人资料请求")
public record UpdateProfileRequest(
        @Schema(description = "姓名", example = "Zhang San")
        @NotBlank
        @Size(max = 64)
        String name,

        @Schema(description = "手机号", example = "13800000001")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "must be a valid mainland China mobile number")
        String phone
) {
}
