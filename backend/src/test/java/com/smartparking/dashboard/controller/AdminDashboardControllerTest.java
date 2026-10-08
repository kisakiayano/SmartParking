package com.smartparking.dashboard.controller;

import com.smartparking.dashboard.service.DashboardService;
import com.smartparking.dashboard.vo.DashboardStatisticsVO;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldGetDashboardStatisticsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(dashboardService.getStatistics()).thenReturn(statistics());

        mockMvc.perform(get("/api/v1/admin/dashboard/statistics")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayIncome").value(10.00))
                .andExpect(jsonPath("$.data.todayParkingCount").value(2))
                .andExpect(jsonPath("$.data.currentOccupancyRate").value(30.00));
    }

    @Test
    void shouldRejectUserOnDashboardEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/dashboard/statistics")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldRejectAnonymousDashboardEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/statistics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
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

    private DashboardStatisticsVO statistics() {
        return new DashboardStatisticsVO(
                new BigDecimal("10.00"),
                2L,
                10L,
                5L,
                2L,
                3L,
                new BigDecimal("30.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
