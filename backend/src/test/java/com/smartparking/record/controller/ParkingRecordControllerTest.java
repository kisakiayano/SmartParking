package com.smartparking.record.controller;

import com.smartparking.common.response.PageResponse;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
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

@WebMvcTest(ParkingRecordController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class ParkingRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private ParkingRecordService parkingRecordService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldGetMineByIdForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(parkingRecordService.getMineById(10L, 1L)).thenReturn(recordVO("PARKING"));

        mockMvc.perform(get("/api/v1/parking-records/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(1));
    }

    @Test
    void shouldPageMineForUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(parkingRecordService.pageMine(any(), any())).thenReturn(new PageResponse<>(List.of(recordVO("PARKING")), 1, 1, 10));

        mockMvc.perform(get("/api/v1/parking-records?status=PARKING")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldRejectAnonymousParkingRecordQuery() throws Exception {
        mockMvc.perform(get("/api/v1/parking-records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowAdminOnUserParkingRecordEndpoint() throws Exception {
        // 角色层级：ADMIN 隐含 USER，管理员同样可访问个人停车记录接口
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));

        mockMvc.perform(get("/api/v1/parking-records")
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

    private ParkingRecordVO recordVO(String status) {
        return new ParkingRecordVO(1L, 100L, 200L, LocalDateTime.now().minusMinutes(30), null, null, null, status, null, null);
    }
}
