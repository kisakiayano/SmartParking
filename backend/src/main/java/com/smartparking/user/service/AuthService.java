package com.smartparking.user.service;

import com.smartparking.user.dto.LoginRequest;
import com.smartparking.user.dto.LogoutRequest;
import com.smartparking.user.dto.RefreshTokenRequest;
import com.smartparking.user.dto.RegisterRequest;
import com.smartparking.user.vo.LoginVO;
import com.smartparking.user.vo.UserVO;

public interface AuthService {

    UserVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    LoginVO refresh(RefreshTokenRequest request);

    void logout(String authorizationHeader, LogoutRequest request);
}
