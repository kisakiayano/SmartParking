package com.smartparking.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "新增车辆请求")
public record CreateVehicleRequest(
        @Schema(description = "车牌号", example = "粤B12345")
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$", message = "must be a valid mainland China license plate number")
        String plateNumber,

        @Schema(description = "车辆类型", example = "SMALL")
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[A-Z_]+$", message = "must contain only uppercase letters and underscores")
        String vehicleType
) {
}
