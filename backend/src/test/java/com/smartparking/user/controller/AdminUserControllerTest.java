package com.smartparking.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
import com.smartparking.user.dto.UpdateUserStatusRequest;
import com.smartparking.user.service.UserService;
import com.smartparking.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminUserControllerTest {

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
    void shouldPageUsersForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(userService.pageForAdmin(any())).thenReturn(new PageResponse<>(List.of(userVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/users?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("user01"));
    }

    @Test
    void shouldUpdateUserStatusForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(userService.updateStatus(any(), any(), any())).thenReturn(disabledUserVO());
        UpdateUserStatusRequest request = new UpdateUserStatusRequest("DISABLED");

        mockMvc.perform(put("/api/v1/admin/users/10/status")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void shouldRejectUserOnAdminUserEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private Claims claims(String username, String role, Long userId) {
        return Jwts.claims()
                .subject(username)
                .add("userId", userId)
                .add("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .build();
    }

    private UserVO userVO() {
        return new UserVO(10L, "user01", "User One", "13800000000", "USER", "ENABLED", null, null);
    }

    private UserVO disabledUserVO() {
        return new UserVO(10L, "user01", "User One", "13800000000", "USER", "DISABLED", null, null);
    }
}
