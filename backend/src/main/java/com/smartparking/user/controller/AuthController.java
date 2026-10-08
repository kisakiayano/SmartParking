package com.smartparking.user.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.user.dto.LoginRequest;
import com.smartparking.user.dto.LogoutRequest;
import com.smartparking.user.dto.RefreshTokenRequest;
import com.smartparking.user.dto.RegisterRequest;
import com.smartparking.user.service.AuthService;
import com.smartparking.user.vo.LoginVO;
import com.smartparking.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌（refresh token 轮换）")
    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ApiResponse<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "登出（吊销当前 access token 与可选的 refresh token）")
    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authorization, request);
        return ApiResponse.success();
    }
}
