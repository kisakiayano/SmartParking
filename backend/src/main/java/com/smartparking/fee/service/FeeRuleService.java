package com.smartparking.fee.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.dto.CreateFeeRuleRequest;
import com.smartparking.fee.dto.FeeRulePageQuery;
import com.smartparking.fee.dto.UpdateFeeRuleRequest;
import com.smartparking.fee.vo.FeeRuleVO;

public interface FeeRuleService {

    FeeRuleVO create(CreateFeeRuleRequest request);

    FeeRuleVO update(Long ruleId, UpdateFeeRuleRequest request);

    FeeRuleVO enable(Long ruleId);

    FeeRuleVO getById(Long ruleId);

    FeeRuleVO getActiveRule();

    PageResponse<FeeRuleVO> page(FeeRulePageQuery query);
}
