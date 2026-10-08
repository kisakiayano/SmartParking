package com.smartparking.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "修改车位请求")
public record UpdateParkingSpaceRequest(
        @Schema(description = "区域", example = "A")
        @NotBlank
        @Size(max = 50)
        String area,

        @Schema(description = "车位类型", example = "STANDARD")
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[A-Z_]+$", message = "must contain only uppercase letters and underscores")
        String type,

        @Schema(description = "车位状态", example = "FREE")
        @NotBlank
        @Pattern(regexp = "^(FREE|RESERVED|OCCUPIED)$", message = "status must be FREE, RESERVED or OCCUPIED")
        String status,

        @Schema(description = "乐观锁版本号", example = "0")
        @NotNull
        Integer version
) {
}
