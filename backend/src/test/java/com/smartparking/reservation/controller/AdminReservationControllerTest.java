package com.smartparking.reservation.controller;

import com.smartparking.common.response.PageResponse;
import com.smartparking.reservation.service.ReservationService;
import com.smartparking.reservation.vo.ReservationVO;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReservationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldPageReservationsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(reservationService.pageForAdmin(any())).thenReturn(new PageResponse<>(List.of(reservationVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/reservations?status=ACTIVE")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));
    }

    @Test
    void shouldRejectUserOnAdminReservationEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/reservations")
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

    private ReservationVO reservationVO() {
        return new ReservationVO(1L, 10L, 100L, 200L, null, LocalDateTime.now().plusHours(1), "ACTIVE", null, null);
    }
}
