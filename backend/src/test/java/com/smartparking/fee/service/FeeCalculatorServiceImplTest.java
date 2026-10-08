package com.smartparking.fee.service;

import com.smartparking.common.exception.BusinessException;
import com.smartparking.fee.entity.FeeRule;
import com.smartparking.fee.mapper.FeeRuleMapper;
import com.smartparking.fee.service.impl.FeeCalculatorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorServiceImplTest {

    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 10, 7, 10, 0);

    @Mock
    private FeeRuleMapper feeRuleMapper;

    @InjectMocks
    private FeeCalculatorServiceImpl feeCalculatorService;

    @Test
    void shouldReturnZeroWithinFreeMinutes() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        BigDecimal result = feeCalculatorService.calculate(ENTRY, ENTRY.plusMinutes(15));

        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRoundUpBillableHours() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        BigDecimal result = feeCalculatorService.calculate(ENTRY, ENTRY.plusMinutes(16));

        assertThat(result).isEqualByComparingTo("5.00");
    }

    @Test
    void shouldCalculateMultipleHours() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        BigDecimal result = feeCalculatorService.calculate(ENTRY, ENTRY.plusMinutes(136));

        assertThat(result).isEqualByComparingTo("15.00");
    }

    @Test
    void shouldCapEachDaySeparatelyAndAccumulateAcrossDays() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        // 从整点跨 3 个自然日：每天 24 小时 → 每日封顶 60，总计 180（旧实现整段封顶只收 60）
        LocalDateTime midnight = LocalDateTime.of(2026, 10, 7, 0, 0);
        BigDecimal result = feeCalculatorService.calculate(midnight, midnight.plusDays(3));

        assertThat(result).isEqualByComparingTo("180.00");
    }

    @Test
    void shouldCapOnlyTheCappedDay() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        // 跨 2 个自然日：第一天 10:00→24:00 计费 825 分钟 = 14 小时 = 70 → 封顶 60；第二天 600 分钟 = 10 小时 = 50
        BigDecimal result = feeCalculatorService.calculate(ENTRY, ENTRY.plusDays(1));

        assertThat(result).isEqualByComparingTo("110.00");
    }

    @Test
    void shouldConsumeFreeMinutesOnlyOnce() {
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule("5.00", 15, "60.00"));

        // 23:00 → 次日 00:10 共 70 分钟：免费时长只在第一天扣一次，次日 10 分钟也要计费 1 小时
        LocalDateTime entry = LocalDateTime.of(2026, 10, 7, 23, 0);
        BigDecimal result = feeCalculatorService.calculate(entry, entry.plusMinutes(70));

        assertThat(result).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectWhenActiveRuleMissing() {
        when(feeRuleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> feeCalculatorService.calculate(ENTRY, ENTRY.plusMinutes(30)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        assertThatThrownBy(() -> feeCalculatorService.calculate(ENTRY, ENTRY))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> feeCalculatorService.calculate(ENTRY, ENTRY.minusMinutes(1)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> feeCalculatorService.calculate(null, ENTRY.plusMinutes(30)))
                .isInstanceOf(BusinessException.class);
    }

    private FeeRule feeRule(String unitPrice, Integer freeMinute, String maxDaily) {
        FeeRule feeRule = new FeeRule();
        feeRule.setRuleId(1L);
        feeRule.setRuleName("默认计费规则");
        feeRule.setUnitPrice(new BigDecimal(unitPrice));
        feeRule.setFreeMinute(freeMinute);
        feeRule.setMaxDaily(new BigDecimal(maxDaily));
        feeRule.setEnabled(true);
        feeRule.setUpdateTime(ENTRY.minusHours(1));
        return feeRule;
    }
}
