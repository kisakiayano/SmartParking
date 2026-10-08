package com.smartparking.parking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.parking.dto.CreateParkingSpaceRequest;
import com.smartparking.parking.dto.UpdateParkingSpaceRequest;
import com.smartparking.parking.service.ParkingSpaceService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminParkingSpaceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminParkingSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private ParkingSpaceService parkingSpaceService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldCreateParkingSpaceForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(parkingSpaceService.create(any())).thenReturn(spaceVO("FREE", 0));

        CreateParkingSpaceRequest request = new CreateParkingSpaceRequest("A", "STANDARD", "FREE");

        mockMvc.perform(post("/api/v1/admin/parking-spaces")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FREE"));
    }

    @Test
    void shouldUpdateParkingSpaceForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(parkingSpaceService.update(any(), any())).thenReturn(spaceVO("RESERVED", 0));

        UpdateParkingSpaceRequest request = new UpdateParkingSpaceRequest("A", "STANDARD", "RESERVED", 0);

        mockMvc.perform(put("/api/v1/admin/parking-spaces/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    @Test
    void shouldDeleteParkingSpaceForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        doNothing().when(parkingSpaceService).delete(any());

        mockMvc.perform(delete("/api/v1/admin/parking-spaces/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectUserOnAdminParkingSpaceEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(delete("/api/v1/admin/parking-spaces/1")
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

    private ParkingSpaceVO spaceVO(String status, Integer version) {
        return new ParkingSpaceVO(1L, "A", "STANDARD", status, version, null, null);
    }
}
