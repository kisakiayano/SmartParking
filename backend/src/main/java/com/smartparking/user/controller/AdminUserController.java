package com.smartparking.user.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.SecurityUtils;
import com.smartparking.user.dto.UpdateUserStatusRequest;
import com.smartparking.user.dto.UserPageQuery;
import com.smartparking.user.service.UserService;
import com.smartparking.user.vo.UserVO;
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

@Tag(name = "管理员用户接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "管理员分页查询用户")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserVO>> page(@Valid UserPageQuery query) {
        return ApiResponse.success(userService.pageForAdmin(query));
    }

    @Operation(summary = "管理员修改用户状态")
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> updateStatus(
            @PathVariable @NotNull Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        Long currentUserId = SecurityUtils.currentUser().userId();
        return ApiResponse.success(userService.updateStatus(currentUserId, userId, request));
    }
}
