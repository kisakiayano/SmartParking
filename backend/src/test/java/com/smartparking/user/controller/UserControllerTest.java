package com.smartparking.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
import com.smartparking.user.dto.ChangePasswordRequest;
import com.smartparking.user.dto.UpdateProfileRequest;
import com.smartparking.user.service.UserService;
import com.smartparking.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRejectCurrentUserWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldGetCurrentUserWithToken() throws Exception {
        when(jwtService.parseClaims("jwt-token")).thenReturn(claims());
        when(userService.getCurrentUser(1L)).thenReturn(userVO());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user01"));
    }

    @Test
    void shouldUpdateProfileWithToken() throws Exception {
        when(jwtService.parseClaims("jwt-token")).thenReturn(claims());
        when(userService.updateProfile(any(), any())).thenReturn(userVO());
        UpdateProfileRequest request = new UpdateProfileRequest("User One", "13800000001");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldChangePasswordWithToken() throws Exception {
        when(jwtService.parseClaims("jwt-token")).thenReturn(claims());
        doNothing().when(userService).changePassword(any(), any());
        ChangePasswordRequest request = new ChangePasswordRequest("Password@123", "NewPassword@123");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Claims claims() {
        return Jwts.claims()
                .subject("user01")
                .add("userId", 1L)
                .add("role", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .build();
    }

    private UserVO userVO() {
        return new UserVO(1L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null);
    }
}
