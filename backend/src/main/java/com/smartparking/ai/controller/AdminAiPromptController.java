package com.smartparking.ai.controller;

import com.smartparking.ai.dto.UpdateAiPromptTemplateRequest;
import com.smartparking.ai.service.AiPromptTemplateService;
import com.smartparking.ai.vo.AiPromptTemplateVO;
import com.smartparking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员 AI 提示词接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/ai/prompts")
@SecurityRequirement(name = "bearerAuth")
public class AdminAiPromptController {

    private final AiPromptTemplateService aiPromptTemplateService;

    public AdminAiPromptController(AiPromptTemplateService aiPromptTemplateService) {
        this.aiPromptTemplateService = aiPromptTemplateService;
    }

    @Operation(summary = "查询 AI 提示词模板")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AiPromptTemplateVO>> list() {
        return ApiResponse.success(aiPromptTemplateService.list());
    }

    @Operation(summary = "查询 AI 提示词模板详情")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AiPromptTemplateVO> getById(@PathVariable @NotNull Long templateId) {
        return ApiResponse.success(aiPromptTemplateService.getById(templateId));
    }

    @Operation(summary = "更新 AI 提示词模板")
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AiPromptTemplateVO> update(
            @PathVariable @NotNull Long templateId,
            @Valid @RequestBody UpdateAiPromptTemplateRequest request
    ) {
        return ApiResponse.success(aiPromptTemplateService.update(templateId, request));
    }
}
