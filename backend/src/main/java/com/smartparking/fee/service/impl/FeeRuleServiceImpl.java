package com.smartparking.fee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.fee.convert.FeeRuleConvert;
import com.smartparking.fee.dto.CreateFeeRuleRequest;
import com.smartparking.fee.dto.FeeRulePageQuery;
import com.smartparking.fee.dto.UpdateFeeRuleRequest;
import com.smartparking.fee.entity.FeeRule;
import com.smartparking.fee.mapper.FeeRuleMapper;
import com.smartparking.fee.service.FeeRuleService;
import com.smartparking.fee.vo.FeeRuleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class FeeRuleServiceImpl implements FeeRuleService {

    private final FeeRuleMapper feeRuleMapper;
    private final FeeRuleConvert feeRuleConvert;

    public FeeRuleServiceImpl(FeeRuleMapper feeRuleMapper, FeeRuleConvert feeRuleConvert) {
        this.feeRuleMapper = feeRuleMapper;
        this.feeRuleConvert = feeRuleConvert;
    }

    @Override
    @Transactional
    public FeeRuleVO create(CreateFeeRuleRequest request) {
        LocalDateTime now = LocalDateTime.now();
        FeeRule feeRule = new FeeRule();
        feeRule.setRuleName(request.ruleName());
        feeRule.setUnitPrice(normalizeMoney(request.unitPrice()));
        feeRule.setFreeMinute(request.freeMinute());
        feeRule.setMaxDaily(normalizeMoney(request.maxDaily()));
        feeRule.setEnabled(Boolean.TRUE.equals(request.enabled()));
        feeRule.setCreateTime(now);
        feeRule.setUpdateTime(now);

        if (Boolean.TRUE.equals(feeRule.getEnabled())) {
            disableAllRules(now);
        }
        feeRuleMapper.insert(feeRule);
        return feeRuleConvert.toVO(feeRule);
    }

    @Override
    @Transactional
    public FeeRuleVO update(Long ruleId, UpdateFeeRuleRequest request) {
        FeeRule feeRule = requireFeeRule(ruleId);
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(request.enabled())) {
            disableOtherRules(ruleId, now);
        }
        feeRule.setRuleName(request.ruleName());
        feeRule.setUnitPrice(normalizeMoney(request.unitPrice()));
        feeRule.setFreeMinute(request.freeMinute());
        feeRule.setMaxDaily(normalizeMoney(request.maxDaily()));
        feeRule.setEnabled(Boolean.TRUE.equals(request.enabled()));
        feeRule.setUpdateTime(now);
        feeRuleMapper.updateById(feeRule);
        return feeRuleConvert.toVO(feeRule);
    }

    @Override
    @Transactional
    public FeeRuleVO enable(Long ruleId) {
        FeeRule feeRule = requireFeeRule(ruleId);
        LocalDateTime now = LocalDateTime.now();
        disableOtherRules(ruleId, now);
        feeRule.setEnabled(true);
        feeRule.setUpdateTime(now);
        feeRuleMapper.updateById(feeRule);
        return feeRuleConvert.toVO(feeRule);
    }

    @Override
    public FeeRuleVO getById(Long ruleId) {
        return feeRuleConvert.toVO(requireFeeRule(ruleId));
    }

    @Override
    public FeeRuleVO getActiveRule() {
        FeeRule feeRule = feeRuleMapper.selectOne(new LambdaQueryWrapper<FeeRule>()
                .eq(FeeRule::getEnabled, true)
                .orderByDesc(FeeRule::getUpdateTime)
                .last("LIMIT 1"));
        if (feeRule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "active fee rule not found");
        }
        return feeRuleConvert.toVO(feeRule);
    }

    @Override
    public PageResponse<FeeRuleVO> page(FeeRulePageQuery query) {
        FeeRulePageQuery normalizedQuery = query == null
                ? new FeeRulePageQuery(null, null, null, null)
                : query;
        LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
        if (normalizedQuery.enabled() != null) {
            wrapper.eq(FeeRule::getEnabled, normalizedQuery.enabled());
        }
        if (StringUtils.hasText(normalizedQuery.ruleName())) {
            wrapper.like(FeeRule::getRuleName, normalizedQuery.ruleName());
        }
        wrapper.orderByDesc(FeeRule::getEnabled).orderByDesc(FeeRule::getCreateTime);

        IPage<FeeRule> page = feeRuleMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                feeRuleConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private FeeRule requireFeeRule(Long ruleId) {
        FeeRule feeRule = feeRuleMapper.selectById(ruleId);
        if (feeRule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "fee rule not found");
        }
        return feeRule;
    }

    private void disableAllRules(LocalDateTime now) {
        feeRuleMapper.update(null, new UpdateWrapper<FeeRule>()
                .eq("enabled", true)
                .set("enabled", false)
                .set("update_time", now));
    }

    private void disableOtherRules(Long ruleId, LocalDateTime now) {
        feeRuleMapper.update(null, new UpdateWrapper<FeeRule>()
                .eq("enabled", true)
                .ne("rule_id", ruleId)
                .set("enabled", false)
                .set("update_time", now));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
