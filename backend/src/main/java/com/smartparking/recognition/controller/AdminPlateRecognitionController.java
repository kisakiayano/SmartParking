package com.smartparking.recognition.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.recognition.dto.PlateRecognitionLogQuery;
import com.smartparking.recognition.service.PlateRecognitionApplicationService;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import com.smartparking.recognition.vo.PlateRecognitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理员车牌识别接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/plate-recognition")
@SecurityRequirement(name = "bearerAuth")
public class AdminPlateRecognitionController {

    private final PlateRecognitionApplicationService recognitionApplicationService;

    public AdminPlateRecognitionController(PlateRecognitionApplicationService recognitionApplicationService) {
        this.recognitionApplicationService = recognitionApplicationService;
    }

    @Operation(summary = "图片上传识别")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlateRecognitionVO> recognize(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(recognitionApplicationService.recognize(file));
    }

    @Operation(summary = "OCR 自动入场")
    @PostMapping(value = "/entry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlateRecognitionVO> recognizeEntry(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "spaceId", required = false) Long spaceId
    ) {
        return ApiResponse.success(recognitionApplicationService.recognizeEntry(file, spaceId));
    }

    @Operation(summary = "OCR 自动出场")
    @PostMapping(value = "/exit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlateRecognitionVO> recognizeExit(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(recognitionApplicationService.recognizeExit(file));
    }

    @Operation(summary = "分页查询车牌识别日志")
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<PlateRecognitionLogVO>> pageLogs(@Valid PlateRecognitionLogQuery query) {
        return ApiResponse.success(recognitionApplicationService.pageLogs(query));
    }
}
