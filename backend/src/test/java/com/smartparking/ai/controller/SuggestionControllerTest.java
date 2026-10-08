package com.smartparking.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.ai.dto.SuggestionCreateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class SuggestionControllerTest {

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
    void shouldCreateSuggestionForAuthenticatedUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(suggestionService.create(eq(10L), any(SuggestionCreateRequest.class))).thenReturn(suggestionVO("PENDING"));

        SuggestionCreateRequest request = new SuggestionCreateRequest(
                "FUNCTION", "希望支持微信支付", "出场缴费时希望增加微信扫码支付。", "13800000000");

        mockMvc.perform(post("/api/v1/suggestions")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.title").value("希望支持微信支付"));
    }

    @Test
    void shouldRejectBlankSuggestionTitle() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        SuggestionCreateRequest request = new SuggestionCreateRequest("FUNCTION", "  ", "内容", null);

        mockMvc.perform(post("/api/v1/suggestions")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectAnonymousSuggestionQuery() throws Exception {
        mockMvc.perform(get("/api/v1/suggestions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldPageMySuggestions() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(suggestionService.pageMine(eq(10L), any()))
                .thenReturn(new PageResponse<>(List.of(suggestionVO("PENDING")), 1, 1, 10));

        mockMvc.perform(get("/api/v1/suggestions?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldGetMySuggestionDetail() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(suggestionService.getMine(10L, 1L)).thenReturn(suggestionVO("RESOLVED"));

        mockMvc.perform(get("/api/v1/suggestions/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
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

    private SuggestionVO suggestionVO(String status) {
        return new SuggestionVO(1L, 10L, "FUNCTION", "希望支持微信支付", "出场缴费时希望增加微信扫码支付。",
                "13800000000", status, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }
}
