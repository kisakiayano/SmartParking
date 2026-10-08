package com.smartparking.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.ai.dto.SuggestionHandleRequest;
import com.smartparking.ai.service.SuggestionService;
import com.smartparking.ai.vo.SuggestionVO;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSuggestionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminSuggestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private SuggestionService suggestionService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldHandleSuggestionForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(suggestionService.handle(eq(1L), eq(1L), any(SuggestionHandleRequest.class)))
                .thenReturn(suggestionVO("RESOLVED", "已记录需求"));

        SuggestionHandleRequest request = new SuggestionHandleRequest("RESOLVED", "已记录需求");

        mockMvc.perform(put("/api/v1/admin/suggestions/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.reply").value("已记录需求"));
    }

    @Test
    void shouldRejectUserOnAdminSuggestionEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/suggestions")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldPageSuggestionsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(suggestionService.pageForAdmin(any()))
                .thenReturn(new PageResponse<>(List.of(suggestionVO("PENDING", null)), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/suggestions?status=PENDING")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldGetSuggestionDetailForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(suggestionService.getForAdmin(1L)).thenReturn(suggestionVO("PENDING", null));

        mockMvc.perform(get("/api/v1/admin/suggestions/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestionId").value("1"));
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

    private SuggestionVO suggestionVO(String status, String reply) {
        return new SuggestionVO(1L, 10L, "FUNCTION", "希望支持微信支付", "出场缴费时希望增加微信扫码支付。",
                "13800000000", status, reply, 1L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}
