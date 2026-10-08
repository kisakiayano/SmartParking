package com.smartparking.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.common.response.PageResponse;
import com.smartparking.reservation.dto.CreateReservationRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldCreateReservationForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(reservationService.create(any(), any())).thenReturn(reservationVO("ACTIVE"));

        CreateReservationRequest request = new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldGetReservationForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(reservationService.getById(10L, 1L)).thenReturn(reservationVO("ACTIVE"));

        mockMvc.perform(get("/api/v1/reservations/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value(1));
    }

    @Test
    void shouldPageMineForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(reservationService.pageMine(any(), any())).thenReturn(new PageResponse<>(List.of(reservationVO("ACTIVE")), 1, 1, 10));

        mockMvc.perform(get("/api/v1/reservations?status=ACTIVE")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldCancelReservationForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        doNothing().when(reservationService).cancel(10L, 1L);

        mockMvc.perform(put("/api/v1/reservations/1/cancel")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldAllowAdminOnUserReservationEndpoint() throws Exception {
        // 角色层级：ADMIN 隐含 USER，管理员同样可访问个人预约接口
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousReservationQuery() throws Exception {
        mockMvc.perform(get("/api/v1/reservations"))
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

    private ReservationVO reservationVO(String status) {
        return new ReservationVO(1L, 10L, 100L, 200L, null, LocalDateTime.now().plusHours(1), status, null, null);
    }
}
