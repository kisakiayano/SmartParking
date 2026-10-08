package com.smartparking.recognition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Schema(description = "车牌识别日志查询参数")
public record PlateRecognitionLogQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "车牌号")
        String plateNumber,

        @Schema(description = "摄像头类型", example = "ENTRY")
        @Pattern(regexp = "^$|^(UPLOAD|ENTRY|EXIT)$", message = "cameraType must be UPLOAD, ENTRY or EXIT")
        String cameraType,

        @Schema(description = "服务商", example = "BAIDU")
        String provider,

        @Schema(description = "识别结果", example = "SUCCESS")
        @Pattern(regexp = "^$|^(SUCCESS|FAILED|LOW_CONFIDENCE)$", message = "result must be SUCCESS, FAILED or LOW_CONFIDENCE")
        String result,

        @Schema(description = "开始时间")
        LocalDateTime startTime,

        @Schema(description = "结束时间")
        LocalDateTime endTime
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
