package com.smartparking.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
import com.smartparking.user.dto.LoginRequest;
import com.smartparking.user.dto.RefreshTokenRequest;
import com.smartparking.user.dto.RegisterRequest;
import com.smartparking.user.service.AuthService;
import com.smartparking.user.vo.LoginVO;
import com.smartparking.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRegister() throws Exception {
        when(authService.register(any())).thenReturn(userVO());

        RegisterRequest request = new RegisterRequest("user01", "Password@123", "User One", "13800000000");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user01"));
    }

    @Test
    void shouldLogin() throws Exception {
        when(authService.login(any())).thenReturn(new LoginVO("jwt-token", "refresh-token", userVO()));

        LoginRequest request = new LoginRequest("user01", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void shouldRefresh() throws Exception {
        when(authService.refresh(any())).thenReturn(new LoginVO("new-jwt-token", "new-refresh-token", userVO()));

        RefreshTokenRequest request = new RefreshTokenRequest("jwt-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.smartparking.user.dto.LogoutRequest("refresh-token"))))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(authService).logout(
                org.mockito.ArgumentMatchers.eq("Bearer jwt-token"),
                any(com.smartparking.user.dto.LogoutRequest.class));
    }

    private UserVO userVO() {
        return new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null);
    }
}
