package com.smartparking.fee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.dto.CreateFeeRuleRequest;
import com.smartparking.fee.dto.UpdateFeeRuleRequest;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFeeRuleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminFeeRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private FeeRuleService feeRuleService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldCreateFeeRuleForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(feeRuleService.create(any())).thenReturn(feeRuleVO(true));

        CreateFeeRuleRequest request = new CreateFeeRuleRequest(
                "默认计费规则",
                new BigDecimal("5.00"),
                15,
                new BigDecimal("60.00"),
                true
        );

        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void shouldUpdateFeeRuleForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(feeRuleService.update(any(), any())).thenReturn(feeRuleVO(true));

        UpdateFeeRuleRequest request = new UpdateFeeRuleRequest(
                "工作日计费规则",
                new BigDecimal("6.00"),
                10,
                new BigDecimal("80.00"),
                true
        );

        mockMvc.perform(put("/api/v1/admin/fee-rules/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unitPrice").value(5.00));
    }

    @Test
    void shouldEnableFeeRuleForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(feeRuleService.enable(1L)).thenReturn(feeRuleVO(true));

        mockMvc.perform(put("/api/v1/admin/fee-rules/1/enable")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void shouldGetFeeRuleForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(feeRuleService.getById(1L)).thenReturn(feeRuleVO(true));

        mockMvc.perform(get("/api/v1/admin/fee-rules/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").value(1));
    }

    @Test
    void shouldPageFeeRulesForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(feeRuleService.page(any())).thenReturn(new PageResponse<>(List.of(feeRuleVO(true)), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/fee-rules?enabled=true")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldRejectUserOnAdminFeeRuleEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/fee-rules")
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

    private FeeRuleVO feeRuleVO(boolean enabled) {
        return new FeeRuleVO(1L, "默认计费规则", new BigDecimal("5.00"), 15, new BigDecimal("60.00"), enabled, null, null);
    }
}
