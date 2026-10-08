package com.smartparking.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
import com.smartparking.vehicle.dto.CreateVehicleRequest;
import com.smartparking.vehicle.dto.UpdateVehicleRequest;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldCreateVehicleForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(vehicleService.create(any(), any())).thenReturn(vehicleVO());

        CreateVehicleRequest request = new CreateVehicleRequest("粤B12345", "SMALL");

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plateNumber").value("粤B12345"));
    }

    @Test
    void shouldUpdateVehicleForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(vehicleService.update(any(), any(), any())).thenReturn(vehicleVO());

        UpdateVehicleRequest request = new UpdateVehicleRequest("粤B12345", "SMALL");

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(1));
    }

    @Test
    void shouldDeleteVehicleForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        doNothing().when(vehicleService).delete(any(), any());

        mockMvc.perform(delete("/api/v1/vehicles/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldListCurrentUserVehicles() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(vehicleService.listByCurrentUser(10L)).thenReturn(List.of(vehicleVO()));

        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].plateNumber").value("粤B12345"));
    }

    @Test
    void shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowAdminOnUserVehicleEndpoint() throws Exception {
        // 角色层级：ADMIN 隐含 USER，管理员同样可访问个人车辆接口
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));

        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
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

    private VehicleVO vehicleVO() {
        return new VehicleVO(1L, "粤B12345", "SMALL", 10L, null, null);
    }
}
