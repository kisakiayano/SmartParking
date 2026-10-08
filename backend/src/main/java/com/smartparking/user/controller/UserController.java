package com.smartparking.user.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.security.SecurityUtils;
import com.smartparking.user.dto.ChangePasswordRequest;
import com.smartparking.user.dto.UpdateProfileRequest;
import com.smartparking.user.service.UserService;
import com.smartparking.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserVO> currentUser() {
        return ApiResponse.success(userService.getCurrentUser(SecurityUtils.currentUser().userId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(SecurityUtils.currentUser().userId(), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.currentUser().userId(), request);
        return ApiResponse.success();
    }
}
