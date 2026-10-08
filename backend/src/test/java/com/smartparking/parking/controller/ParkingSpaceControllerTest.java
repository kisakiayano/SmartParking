package com.smartparking.parking.controller;

import com.smartparking.common.response.PageResponse;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;
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
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpaceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class ParkingSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private ParkingSpaceService parkingSpaceService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldPageParkingSpacesForAuthenticatedUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(parkingSpaceService.page(any())).thenReturn(new PageResponse<>(List.of(spaceVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/parking-spaces?area=A&status=FREE")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("FREE"));
    }

    @Test
    void shouldGetParkingSpaceByIdForAuthenticatedUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(parkingSpaceService.getById(1L)).thenReturn(spaceVO());

        mockMvc.perform(get("/api/v1/parking-spaces/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spaceId").value(1));
    }

    @Test
    void shouldGetRealtimeStatusForAuthenticatedUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(parkingSpaceService.getRealtimeStatus(1L)).thenReturn(new ParkingSpaceStatusVO(1L, "A", "STANDARD", "FREE", 0));

        mockMvc.perform(get("/api/v1/parking-spaces/1/status")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FREE"));
    }

    @Test
    void shouldRejectAnonymousParkingSpaceQuery() throws Exception {
        mockMvc.perform(get("/api/v1/parking-spaces"))
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

    private ParkingSpaceVO spaceVO() {
        return new ParkingSpaceVO(1L, "A", "STANDARD", "FREE", 0, null, null);
    }
}
