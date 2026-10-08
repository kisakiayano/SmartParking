package com.smartparking.ai.support;

import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.provider.AiContextPayload;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceVO;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import com.smartparking.reservation.service.ReservationService;
import com.smartparking.reservation.vo.ReservationVO;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantContextBuilderTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private ParkingRecordService parkingRecordService;

    @Mock
    private ParkingSpaceService parkingSpaceService;

    @Mock
    private FeeRuleService feeRuleService;

    @InjectMocks
    private AssistantContextBuilder contextBuilder;

    @Test
    void shouldDescribeUserVehicles() {
        when(vehicleService.listByCurrentUser(10L)).thenReturn(List.of(
                new VehicleVO(100L, "沪A12345", "STANDARD", 10L, null, null),
                new VehicleVO(101L, "沪B54321", "SUV", 10L, null, null)));

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.MY_VEHICLES);

        assertThat(payload.answers()).hasSize(1);
        assertThat(payload.answers().get(0)).contains("2 辆车").contains("沪A12345").contains("沪B54321");
        assertThat(payload.referencedData()).containsExactly("车辆数据：2 条记录");
    }

    @Test
    void shouldPromptVehicleRegistrationWhenEmpty() {
        when(vehicleService.listByCurrentUser(10L)).thenReturn(List.of());

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.MY_VEHICLES);

        assertThat(payload.answers().get(0)).contains("还没有登记车辆");
        assertThat(payload.referencedData()).isEmpty();
    }

    @Test
    void shouldSummarizeParkingSpaceStatus() {
        when(parkingSpaceService.page(any(ParkingSpaceQuery.class))).thenAnswer(invocation -> {
            ParkingSpaceQuery query = invocation.getArgument(0);
            if (query.status() == null) {
                return new PageResponse<ParkingSpaceVO>(List.of(), 10, 1, 1);
            }
            if ("FREE".equals(query.status())) {
                return new PageResponse<>(
                        List.of(new ParkingSpaceVO(200L, "A", "STANDARD", "FREE", 0, null, null)),
                        4,
                        1,
                        5);
            }
            return new PageResponse<ParkingSpaceVO>(List.of(), 2, 1, 1);
        });

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.SPACE_STATUS);

        assertThat(payload.answers().get(0)).contains("空闲 4 个").contains("已预约 2 个").contains("占用 4 个");
        assertThat(payload.referencedData()).contains("车位总数：10", "空闲：4", "已预约：2", "占用：4");
    }

    @Test
    void shouldDescribeActiveReservations() {
        LocalDateTime arriveTime = LocalDateTime.of(2026, 9, 24, 10, 30);
        when(reservationService.pageMine(any(), any())).thenReturn(new PageResponse<>(
                List.of(new ReservationVO(300L, 10L, 100L, 200L, null, arriveTime, "ACTIVE", null, null)),
                1,
                1,
                5));

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.MY_RESERVATIONS);

        assertThat(payload.answers().get(0)).contains("1 条生效中的预约").contains("车位 200").contains("09-24 10:30");
        assertThat(payload.referencedData()).containsExactly("生效预约：1 条");
    }

    @Test
    void shouldDescribeLatestParkingRecords() {
        LocalDateTime entryTime = LocalDateTime.of(2026, 9, 24, 8, 0);
        when(parkingRecordService.pageMine(any(), any(ParkingRecordPageQuery.class))).thenReturn(new PageResponse<>(
                List.of(new ParkingRecordVO(400L, 100L, 200L, entryTime, null, null, null, "PARKING", null, null)),
                1,
                1,
                5));

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.MY_PARKING_RECORDS);

        assertThat(payload.answers().get(0)).contains("目前仍在停车中").contains("09-24 08:00");
    }

    @Test
    void shouldDescribeActiveFeeRule() {
        when(feeRuleService.getActiveRule()).thenReturn(new FeeRuleVO(
                1L, "默认计费规则", new BigDecimal("5.00"), 15, new BigDecimal("60.00"), true, null, null));

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.FEE_RULE);

        assertThat(payload.answers().get(0)).contains("默认计费规则").contains("每小时 5.00 元").contains("15 分钟");
        assertThat(payload.referencedData()).containsExactly("计费规则：默认计费规则");
    }

    @Test
    void shouldHandleMissingFeeRule() {
        when(feeRuleService.getActiveRule()).thenThrow(new BusinessException(ResultCode.NOT_FOUND, "active fee rule not found"));

        AiContextPayload payload = contextBuilder.build(10L, AiIntent.FEE_RULE);

        assertThat(payload.answers().get(0)).contains("还未配置生效的计费规则");
        assertThat(payload.referencedData()).isEmpty();
    }
}
