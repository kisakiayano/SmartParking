package com.smartparking.fee.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "计费规则接口")
@RestController
@RequestMapping("/api/v1/fee-rules")
@SecurityRequirement(name = "bearerAuth")
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    public FeeRuleController(FeeRuleService feeRuleService) {
        this.feeRuleService = feeRuleService;
    }

    @Operation(summary = "查询启用计费规则")
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FeeRuleVO> getActiveRule() {
        return ApiResponse.success(feeRuleService.getActiveRule());
    }
}
