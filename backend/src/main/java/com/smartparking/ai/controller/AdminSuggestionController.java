package com.smartparking.ai.controller;

import com.smartparking.ai.dto.SuggestionHandleRequest;
import com.smartparking.ai.dto.SuggestionPageQuery;
import com.smartparking.ai.service.SuggestionService;
import com.smartparking.ai.vo.SuggestionVO;
import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.AuthUser;
import com.smartparking.security.SecurityUtils;
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

@Tag(name = "管理员用户建议接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/suggestions")
@SecurityRequirement(name = "bearerAuth")
public class AdminSuggestionController {

    private final SuggestionService suggestionService;

    public AdminSuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Operation(summary = "分页查询用户建议")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<SuggestionVO>> page(@Valid SuggestionPageQuery query) {
        return ApiResponse.success(suggestionService.pageForAdmin(query));
    }

    @Operation(summary = "查询用户建议详情")
    @GetMapping("/{suggestionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SuggestionVO> getById(@PathVariable @NotNull Long suggestionId) {
        return ApiResponse.success(suggestionService.getForAdmin(suggestionId));
    }

    @Operation(summary = "处理用户建议")
    @PutMapping("/{suggestionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SuggestionVO> handle(
            @PathVariable @NotNull Long suggestionId,
            @Valid @RequestBody SuggestionHandleRequest request
    ) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(suggestionService.handle(authUser.userId(), suggestionId, request));
    }
}
