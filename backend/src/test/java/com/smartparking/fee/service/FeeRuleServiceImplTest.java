package com.smartparking.fee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.convert.FeeRuleConvert;
import com.smartparking.fee.dto.CreateFeeRuleRequest;
import com.smartparking.fee.dto.FeeRulePageQuery;
import com.smartparking.fee.dto.UpdateFeeRuleRequest;
import com.smartparking.fee.entity.FeeRule;
import com.smartparking.fee.mapper.FeeRuleMapper;
import com.smartparking.fee.service.impl.FeeRuleServiceImpl;
import com.smartparking.fee.vo.FeeRuleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeRuleServiceImplTest {

    @Mock
    private FeeRuleMapper feeRuleMapper;

    @Mock
    private FeeRuleConvert feeRuleConvert;

    @InjectMocks
    private FeeRuleServiceImpl feeRuleService;

    @Test
    void shouldCreateEnabledFeeRuleAndDisableOthers() {
        when(feeRuleConvert.toVO(any())).thenReturn(feeRuleVO(1L, true));

        FeeRuleVO result = feeRuleService.create(new CreateFeeRuleRequest(
                "默认计费规则",
                new BigDecimal("5"),
                15,
                new BigDecimal("60"),
                true
        ));

        ArgumentCaptor<FeeRule> captor = ArgumentCaptor.forClass(FeeRule.class);
        verify(feeRuleMapper).update(any(), any());
        verify(feeRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo("5.00");
        assertThat(captor.getValue().getMaxDaily()).isEqualByComparingTo("60.00");
        assertThat(captor.getValue().getEnabled()).isTrue();
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldUpdateEnabledFeeRuleAndDisableOthers() {
        FeeRule feeRule = feeRule(1L, false);
        when(feeRuleMapper.selectById(1L)).thenReturn(feeRule);
        when(feeRuleConvert.toVO(feeRule)).thenReturn(feeRuleVO(1L, true));

        FeeRuleVO result = feeRuleService.update(1L, new UpdateFeeRuleRequest(
                "工作日计费规则",
                new BigDecimal("6"),
                10,
                new BigDecimal("80"),
                true
        ));

        verify(feeRuleMapper).update(any(), any());
        verify(feeRuleMapper).updateById(feeRule);
        assertThat(feeRule.getRuleName()).isEqualTo("工作日计费规则");
        assertThat(feeRule.getUnitPrice()).isEqualByComparingTo("6.00");
        assertThat(feeRule.getEnabled()).isTrue();
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldEnableFeeRule() {
        FeeRule feeRule = feeRule(1L, false);
        when(feeRuleMapper.selectById(1L)).thenReturn(feeRule);
        when(feeRuleConvert.toVO(feeRule)).thenReturn(feeRuleVO(1L, true));

        FeeRuleVO result = feeRuleService.enable(1L);

        verify(feeRuleMapper).update(any(), any());
        verify(feeRuleMapper).updateById(feeRule);
        assertThat(feeRule.getEnabled()).isTrue();
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldGetActiveRule() {
        FeeRule feeRule = feeRule(1L, true);
        when(feeRuleMapper.selectOne(any())).thenReturn(feeRule);
        when(feeRuleConvert.toVO(feeRule)).thenReturn(feeRuleVO(1L, true));

        FeeRuleVO result = feeRuleService.getActiveRule();

        assertThat(result.ruleId()).isEqualTo(1L);
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void shouldRejectMissingActiveRule() {
        when(feeRuleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> feeRuleService.getActiveRule())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPageFeeRules() {
        FeeRule feeRule = feeRule(1L, true);
        Page<FeeRule> page = Page.of(1, 10);
        page.setRecords(List.of(feeRule));
        page.setTotal(1);
        when(feeRuleMapper.selectPage(any(), any())).thenReturn(page);
        when(feeRuleConvert.toVOList(List.of(feeRule))).thenReturn(List.of(feeRuleVO(1L, true)));

        PageResponse<FeeRuleVO> result = feeRuleService.page(new FeeRulePageQuery(1L, 10L, true, "默认"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
    }

    @Test
    void shouldRejectMissingFeeRule() {
        when(feeRuleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> feeRuleService.enable(1L))
                .isInstanceOf(BusinessException.class);
    }

    private FeeRule feeRule(Long ruleId, boolean enabled) {
        FeeRule feeRule = new FeeRule();
        feeRule.setRuleId(ruleId);
        feeRule.setRuleName("默认计费规则");
        feeRule.setUnitPrice(new BigDecimal("5.00"));
        feeRule.setFreeMinute(15);
        feeRule.setMaxDaily(new BigDecimal("60.00"));
        feeRule.setEnabled(enabled);
        return feeRule;
    }

    private FeeRuleVO feeRuleVO(Long ruleId, boolean enabled) {
        return new FeeRuleVO(ruleId, "默认计费规则", new BigDecimal("5.00"), 15, new BigDecimal("60.00"), enabled, null, null);
    }
}
