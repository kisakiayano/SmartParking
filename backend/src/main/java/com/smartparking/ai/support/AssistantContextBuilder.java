package com.smartparking.ai.support;

import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.provider.AiContextPayload;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.enums.ParkingRecordStatus;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import com.smartparking.reservation.dto.ReservationPageQuery;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.service.ReservationService;
import com.smartparking.reservation.vo.ReservationVO;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceVO;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 汇总实时业务数据，为 AI 回答提供可信上下文。
 */
@Component
public class AssistantContextBuilder {

    private static final int MAX_ITEMS = 5;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final VehicleService vehicleService;
    private final ReservationService reservationService;
    private final ParkingRecordService parkingRecordService;
    private final ParkingSpaceService parkingSpaceService;
    private final FeeRuleService feeRuleService;

    public AssistantContextBuilder(
            VehicleService vehicleService,
            ReservationService reservationService,
            ParkingRecordService parkingRecordService,
            ParkingSpaceService parkingSpaceService,
            FeeRuleService feeRuleService
    ) {
        this.vehicleService = vehicleService;
        this.reservationService = reservationService;
        this.parkingRecordService = parkingRecordService;
        this.parkingSpaceService = parkingSpaceService;
        this.feeRuleService = feeRuleService;
    }

    public AiContextPayload build(Long userId, AiIntent intent) {
        return switch (intent) {
            case MY_VEHICLES -> buildVehicleAnswer(userId);
            case SPACE_STATUS -> buildSpaceAnswer();
            case MY_RESERVATIONS -> buildReservationAnswer(userId);
            case MY_PARKING_RECORDS -> buildParkingRecordAnswer(userId);
            case FEE_RULE -> buildFeeAnswer();
            default -> AiContextPayload.empty();
        };
    }

    private AiContextPayload buildVehicleAnswer(Long userId) {
        List<VehicleVO> vehicles = vehicleService.listByCurrentUser(userId);
        if (vehicles.isEmpty()) {
            return new AiContextPayload(
                    List.of("您还没有登记车辆。请先前往「我的车辆」添加车牌号和车型，之后即可使用预约和停车服务。"),
                    List.of());
        }
        String detail = vehicles.stream()
                .limit(MAX_ITEMS)
                .map(vehicle -> vehicle.plateNumber() + "（" + vehicle.vehicleType() + "）")
                .collect(Collectors.joining("、"));
        String suffix = vehicles.size() > MAX_ITEMS ? " 等" : "";
        return new AiContextPayload(
                List.of("您当前登记了 " + vehicles.size() + " 辆车：" + detail + suffix + "。"),
                List.of("车辆数据：" + vehicles.size() + " 条记录"));
    }

    private AiContextPayload buildSpaceAnswer() {
        long total = parkingSpaceService.page(new ParkingSpaceQuery(1L, 1L, null, null, null)).total();
        PageResponse<ParkingSpaceVO> freePage = parkingSpaceService.page(
                new ParkingSpaceQuery(1L, (long) MAX_ITEMS, null, null, ParkingSpaceStatus.FREE.name()));
        long reserved = parkingSpaceService.page(
                new ParkingSpaceQuery(1L, 1L, null, null, ParkingSpaceStatus.RESERVED.name())).total();
        long free = freePage.total();
        long occupied = Math.max(0, total - free - reserved);

        StringBuilder answer = new StringBuilder()
                .append("当前停车场共有 ").append(total)
                .append(" 个车位：空闲 ").append(free)
                .append(" 个，已预约 ").append(reserved)
                .append(" 个，占用 ").append(occupied).append(" 个。");
        if (!freePage.records().isEmpty()) {
            String examples = freePage.records().stream()
                    .limit(MAX_ITEMS)
                    .map(space -> space.area() + " 区 " + space.spaceId())
                    .collect(Collectors.joining("、"));
            answer.append("空闲示例：").append(examples).append("。");
        }
        if (free == 0) {
            answer.append("当前没有空闲车位，建议稍后再试或提前在线预约。");
        }
        return new AiContextPayload(
                List.of(answer.toString()),
                List.of("车位总数：" + total, "空闲：" + free, "已预约：" + reserved, "占用：" + occupied));
    }

    private AiContextPayload buildReservationAnswer(Long userId) {
        PageResponse<ReservationVO> activePage = reservationService.pageMine(userId,
                new ReservationPageQuery(1L, (long) MAX_ITEMS, ReservationStatus.ACTIVE.name(), null, null, null));
        if (activePage.total() == 0 || activePage.records().isEmpty()) {
            return new AiContextPayload(
                    List.of("您当前没有生效中的预约。可以前往「在线预约」选择空闲车位和车辆进行预约。"),
                    List.of());
        }
        String detail = activePage.records().stream()
                .limit(MAX_ITEMS)
                .map(reservation -> "车位 " + reservation.spaceId() + "（预计到达 "
                        + formatTime(reservation.arriveTime()) + "）")
                .collect(Collectors.joining("、"));
        return new AiContextPayload(
                List.of("您当前有 " + activePage.total() + " 条生效中的预约：" + detail + "。"),
                List.of("生效预约：" + activePage.total() + " 条"));
    }

    private AiContextPayload buildParkingRecordAnswer(Long userId) {
        PageResponse<ParkingRecordVO> recordPage = parkingRecordService.pageMine(userId,
                new ParkingRecordPageQuery(1L, (long) MAX_ITEMS, null, null, null));
        if (recordPage.total() == 0 || recordPage.records().isEmpty()) {
            return new AiContextPayload(
                    List.of("您还没有停车记录。车辆入场后系统会自动生成停车记录。"),
                    List.of());
        }
        String detail = recordPage.records().stream()
                .limit(MAX_ITEMS)
                .map(this::describeRecord)
                .collect(Collectors.joining("；"));
        return new AiContextPayload(
                List.of("最近 " + recordPage.records().size() + " 条停车记录：" + detail + "。"),
                List.of("停车记录：" + recordPage.total() + " 条"));
    }

    private AiContextPayload buildFeeAnswer() {
        FeeRuleVO feeRule;
        try {
            feeRule = feeRuleService.getActiveRule();
        } catch (BusinessException ex) {
            return new AiContextPayload(
                    List.of("当前还未配置生效的计费规则，请联系管理员配置后再查看费用信息。"),
                    List.of());
        }
        String answer = "当前生效的计费规则为「" + feeRule.ruleName() + "」：每小时 " + feeRule.unitPrice()
                + " 元，免费时长 " + feeRule.freeMinute() + " 分钟，日封顶 " + feeRule.maxDaily()
                + " 元。停车时长不超过免费时长不收费；超出部分按小时向上取整计费，并与日封顶金额取较小值。";
        return new AiContextPayload(List.of(answer), List.of("计费规则：" + feeRule.ruleName()));
    }

    private String describeRecord(ParkingRecordVO record) {
        StringBuilder description = new StringBuilder("车辆 ")
                .append(record.vehicleId())
                .append(" 于 ")
                .append(formatTime(record.entryTime()))
                .append(" 入场");
        if (ParkingRecordStatus.PARKING.name().equals(record.status())) {
            description.append("，目前仍在停车中");
        } else {
            description.append("，时长 ").append(formatDuration(record.duration()))
                    .append("，费用 ").append(record.fee() == null ? "0.00" : record.fee()).append(" 元");
        }
        return description.toString();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "未知时间" : TIME_FORMATTER.format(time);
    }

    private String formatDuration(Integer duration) {
        if (duration == null || duration <= 0) {
            return "不足 1 分钟";
        }
        if (duration < 60) {
            return duration + " 分钟";
        }
        int hours = duration / 60;
        int minutes = duration % 60;
        return minutes == 0 ? hours + " 小时" : hours + " 小时 " + minutes + " 分钟";
    }
}
