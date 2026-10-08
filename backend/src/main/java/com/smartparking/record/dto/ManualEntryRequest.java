package com.smartparking.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "人工入场登记请求")
public record ManualEntryRequest(
        @Schema(description = "车牌号", example = "粤B12345")
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$", message = "must be a valid mainland China license plate number")
        String plateNumber,

        @Schema(description = "车位 ID", example = "2001")
        @NotNull
        Long spaceId
) {
}
