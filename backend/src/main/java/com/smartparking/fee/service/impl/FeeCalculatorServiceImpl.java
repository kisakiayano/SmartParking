package com.smartparking.fee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import com.smartparking.fee.entity.FeeRule;
import com.smartparking.fee.mapper.FeeRuleMapper;
import com.smartparking.fee.service.FeeCalculatorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class FeeCalculatorServiceImpl implements FeeCalculatorService {

    private final FeeRuleMapper feeRuleMapper;

    public FeeCalculatorServiceImpl(FeeRuleMapper feeRuleMapper) {
        this.feeRuleMapper = feeRuleMapper;
    }

    @Override
    public BigDecimal calculate(LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null || !exitTime.isAfter(entryTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "exit time must be after entry time");
        }
        FeeRule activeRule = selectActiveRule();
        return calculate(entryTime, exitTime, activeRule).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculate(LocalDateTime entryTime, LocalDateTime exitTime, FeeRule rule) {
        if (Duration.between(entryTime, exitTime).toMinutes() <= rule.getFreeMinute()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        long remainingFreeMinutes = rule.getFreeMinute();
        LocalDateTime segmentStart = entryTime;
        while (segmentStart.isBefore(exitTime)) {
            LocalDateTime segmentEnd = minOf(endOfDay(segmentStart), exitTime);
            long minutes = Duration.between(segmentStart, segmentEnd).toMinutes();
            long billableMinutes = Math.max(0, minutes - remainingFreeMinutes);
            remainingFreeMinutes = Math.max(0, remainingFreeMinutes - minutes);
            if (billableMinutes > 0) {
                long billableHours = (billableMinutes + 59) / 60;
                BigDecimal dayFee = rule.getUnitPrice().multiply(BigDecimal.valueOf(billableHours));
                total = total.add(dayFee.min(rule.getMaxDaily()));
            }
            segmentStart = segmentEnd;
        }
        return total;
    }

    private FeeRule selectActiveRule() {
        FeeRule activeRule = feeRuleMapper.selectOne(new LambdaQueryWrapper<FeeRule>()
                .eq(FeeRule::getEnabled, true)
                .orderByDesc(FeeRule::getUpdateTime)
                .last("LIMIT 1"));
        if (activeRule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "active fee rule not found");
        }
        return activeRule;
    }

    private LocalDateTime endOfDay(LocalDateTime time) {
        return time.toLocalDate().plusDays(1).atStartOfDay();
    }

    private LocalDateTime minOf(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
