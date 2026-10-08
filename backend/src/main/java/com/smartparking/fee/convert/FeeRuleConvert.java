package com.smartparking.fee.convert;

import com.smartparking.fee.entity.FeeRule;
import com.smartparking.fee.vo.FeeRuleVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FeeRuleConvert {

    FeeRuleVO toVO(FeeRule feeRule);

    List<FeeRuleVO> toVOList(List<FeeRule> feeRules);
}
