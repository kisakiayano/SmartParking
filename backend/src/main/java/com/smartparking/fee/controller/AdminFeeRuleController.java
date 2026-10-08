package com.smartparking.fee.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.dto.CreateFeeRuleRequest;
import com.smartparking.fee.dto.FeeRulePageQuery;
import com.smartparking.fee.dto.UpdateFeeRuleRequest;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员计费规则接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/fee-rules")
@SecurityRequirement(name = "bearerAuth")
public class AdminFeeRuleController {

    private final FeeRuleService feeRuleService;

    public AdminFeeRuleController(FeeRuleService feeRuleService) {
        this.feeRuleService = feeRuleService;
    }

    @Operation(summary = "新增计费规则")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeRuleVO> create(@Valid @RequestBody CreateFeeRuleRequest request) {
        return ApiResponse.success(feeRuleService.create(request));
    }

    @Operation(summary = "修改计费规则")
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeRuleVO> update(
            @PathVariable @NotNull Long ruleId,
            @Valid @RequestBody UpdateFeeRuleRequest request
    ) {
        return ApiResponse.success(feeRuleService.update(ruleId, request));
    }

    @Operation(summary = "启用计费规则")
    @PutMapping("/{ruleId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeRuleVO> enable(@PathVariable @NotNull Long ruleId) {
        return ApiResponse.success(feeRuleService.enable(ruleId));
    }

    @Operation(summary = "查询计费规则详情")
    @GetMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeeRuleVO> getById(@PathVariable @NotNull Long ruleId) {
        return ApiResponse.success(feeRuleService.getById(ruleId));
    }

    @Operation(summary = "分页查询计费规则")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<FeeRuleVO>> page(@Valid FeeRulePageQuery query) {
        return ApiResponse.success(feeRuleService.page(query));
    }
}
