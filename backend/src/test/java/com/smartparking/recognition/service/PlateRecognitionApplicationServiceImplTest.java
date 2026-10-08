package com.smartparking.recognition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.recognition.convert.PlateRecognitionLogConvert;
import com.smartparking.recognition.dto.PlateRecognitionLogQuery;
import com.smartparking.recognition.entity.PlateRecognitionLog;
import com.smartparking.recognition.mapper.PlateRecognitionLogMapper;
import com.smartparking.recognition.model.PlateRecognitionResult;
import com.smartparking.recognition.service.impl.PlateRecognitionApplicationServiceImpl;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import com.smartparking.recognition.vo.PlateRecognitionVO;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateRecognitionApplicationServiceImplTest {

    @Mock
    private PlateRecognitionService plateRecognitionService;

    @Mock
    private ParkingRecordService parkingRecordService;

    @Mock
    private PlateRecognitionLogMapper logMapper;

    @Mock
    private PlateRecognitionLogConvert logConvert;

    @InjectMocks
    private PlateRecognitionApplicationServiceImpl recognitionApplicationService;

    @Test
    void shouldRecognizeUploadedImage() {
        when(plateRecognitionService.recognize(any())).thenReturn(successResult(true));

        PlateRecognitionVO result = recognitionApplicationService.recognize(file());

        ArgumentCaptor<PlateRecognitionLog> captor = ArgumentCaptor.forClass(PlateRecognitionLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getCameraType()).isEqualTo("UPLOAD");
        assertThat(captor.getValue().getResult()).isEqualTo("SUCCESS");
        assertThat(result.plateNumber()).isEqualTo("粤B12345");
    }

    @Test
    void shouldAutoEntryAfterRecognition() {
        ParkingRecordVO recordVO = parkingRecordVO("PARKING");
        when(plateRecognitionService.recognize(any())).thenReturn(successResult(true));
        when(parkingRecordService.autoEntry("粤B12345", 200L)).thenReturn(recordVO);

        PlateRecognitionVO result = recognitionApplicationService.recognizeEntry(file(), 200L);

        ArgumentCaptor<PlateRecognitionLog> captor = ArgumentCaptor.forClass(PlateRecognitionLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getCameraType()).isEqualTo("ENTRY");
        assertThat(captor.getValue().getRecordId()).isEqualTo(1L);
        assertThat(result.parkingRecord().status()).isEqualTo("PARKING");
    }

    @Test
    void shouldAutoExitAfterRecognition() {
        ParkingRecordVO recordVO = parkingRecordVO("FINISHED");
        when(plateRecognitionService.recognize(any())).thenReturn(successResult(true));
        when(parkingRecordService.autoExit("粤B12345")).thenReturn(recordVO);

        PlateRecognitionVO result = recognitionApplicationService.recognizeExit(file());

        ArgumentCaptor<PlateRecognitionLog> captor = ArgumentCaptor.forClass(PlateRecognitionLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getCameraType()).isEqualTo("EXIT");
        assertThat(captor.getValue().getRecordId()).isEqualTo(1L);
        assertThat(result.parkingRecord().status()).isEqualTo("FINISHED");
    }

    @Test
    void shouldLogLowConfidenceAndRejectEntry() {
        when(plateRecognitionService.recognize(any())).thenReturn(successResult(false));

        assertThatThrownBy(() -> recognitionApplicationService.recognizeEntry(file(), null))
                .isInstanceOf(BusinessException.class);

        ArgumentCaptor<PlateRecognitionLog> captor = ArgumentCaptor.forClass(PlateRecognitionLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("LOW_CONFIDENCE");
        assertThat(captor.getValue().getErrorCode()).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    void shouldLogFailedRecognitionAndRejectExit() {
        when(plateRecognitionService.recognize(any())).thenReturn(PlateRecognitionResult.failed("BAIDU", "log-1", "BAIDU_ERROR", "recognition failed"));

        assertThatThrownBy(() -> recognitionApplicationService.recognizeExit(file()))
                .isInstanceOf(BusinessException.class);

        ArgumentCaptor<PlateRecognitionLog> captor = ArgumentCaptor.forClass(PlateRecognitionLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorCode()).isEqualTo("BAIDU_ERROR");
    }

    @Test
    void shouldPageLogs() {
        PlateRecognitionLog log = new PlateRecognitionLog();
        Page<PlateRecognitionLog> page = Page.of(1, 10);
        page.setRecords(List.of(log));
        page.setTotal(1);
        when(logMapper.selectPage(any(), any())).thenReturn(page);
        when(logConvert.toVOList(List.of(log))).thenReturn(List.of(logVO()));

        PageResponse<PlateRecognitionLogVO> result = recognitionApplicationService.pageLogs(
                new PlateRecognitionLogQuery(1L, 10L, "粤B12345", "ENTRY", "BAIDU", "SUCCESS", null, null)
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "plate.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private PlateRecognitionResult successResult(boolean confidenceQualified) {
        return PlateRecognitionResult.success("粤B12345", "blue", new BigDecimal("0.9821"), "BAIDU", "log-1", confidenceQualified);
    }

    private ParkingRecordVO parkingRecordVO(String status) {
        return new ParkingRecordVO(1L, 100L, 200L, LocalDateTime.now().minusMinutes(30), null, null, null, status, null, null);
    }

    private PlateRecognitionLogVO logVO() {
        return new PlateRecognitionLogVO(1L, 1L, "粤B12345", "blue", new BigDecimal("0.9821"), "plate.jpg", LocalDateTime.now(), "ENTRY", "BAIDU", "log-1", "SUCCESS", null, null, null);
    }
}
