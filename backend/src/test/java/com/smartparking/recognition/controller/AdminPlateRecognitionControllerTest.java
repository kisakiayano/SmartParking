package com.smartparking.recognition.controller;

import com.smartparking.common.response.PageResponse;
import com.smartparking.recognition.service.PlateRecognitionApplicationService;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import com.smartparking.recognition.vo.PlateRecognitionVO;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPlateRecognitionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminPlateRecognitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private PlateRecognitionApplicationService recognitionApplicationService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRecognizeUploadedImageForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(recognitionApplicationService.recognize(any())).thenReturn(recognitionVO(null));

        mockMvc.perform(multipart("/api/v1/admin/plate-recognition/recognize")
                        .file(file())
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plateNumber").value("粤B12345"));
    }

    @Test
    void shouldAutoEntryForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(recognitionApplicationService.recognizeEntry(any(), any())).thenReturn(recognitionVO(parkingRecordVO("PARKING")));

        mockMvc.perform(multipart("/api/v1/admin/plate-recognition/entry")
                        .file(file())
                        .param("spaceId", "200")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parkingRecord.status").value("PARKING"));
    }

    @Test
    void shouldAutoExitForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(recognitionApplicationService.recognizeExit(any())).thenReturn(recognitionVO(parkingRecordVO("FINISHED")));

        mockMvc.perform(multipart("/api/v1/admin/plate-recognition/exit")
                        .file(file())
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parkingRecord.status").value("FINISHED"));
    }

    @Test
    void shouldPageLogsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(recognitionApplicationService.pageLogs(any())).thenReturn(new PageResponse<>(List.of(logVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/plate-recognition/logs?cameraType=ENTRY")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldRejectUserOnRecognitionEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(multipart("/api/v1/admin/plate-recognition/entry")
                        .file(file())
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldRejectAnonymousRecognitionEndpoint() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/plate-recognition/entry")
                        .file(file()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "plate.jpg", "image/jpeg", new byte[]{1, 2, 3});
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

    private PlateRecognitionVO recognitionVO(ParkingRecordVO parkingRecord) {
        return new PlateRecognitionVO("粤B12345", "blue", new BigDecimal("0.9821"), "BAIDU", "log-1", 1L, parkingRecord);
    }

    private ParkingRecordVO parkingRecordVO(String status) {
        return new ParkingRecordVO(1L, 100L, 200L, LocalDateTime.now().minusMinutes(30), null, null, null, status, null, null);
    }

    private PlateRecognitionLogVO logVO() {
        return new PlateRecognitionLogVO(1L, 1L, "粤B12345", "blue", new BigDecimal("0.9821"), "plate.jpg", LocalDateTime.now(), "ENTRY", "BAIDU", "log-1", "SUCCESS", null, null, null);
    }
}
