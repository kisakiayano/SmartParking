package com.smartparking.ai.controller;

import com.smartparking.ai.dto.SuggestionCreateRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户建议接口")
@Validated
@RestController
@RequestMapping("/api/v1/suggestions")
@SecurityRequirement(name = "bearerAuth")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Operation(summary = "提交建议")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SuggestionVO> create(@Valid @RequestBody SuggestionCreateRequest request) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(suggestionService.create(authUser.userId(), request));
    }

    @Operation(summary = "分页查询我的建议")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<SuggestionVO>> pageMine(@Valid SuggestionPageQuery query) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(suggestionService.pageMine(authUser.userId(), query));
    }

    @Operation(summary = "查询我的建议详情")
    @GetMapping("/{suggestionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SuggestionVO> getMine(@PathVariable @NotNull Long suggestionId) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(suggestionService.getMine(authUser.userId(), suggestionId));
    }
}
