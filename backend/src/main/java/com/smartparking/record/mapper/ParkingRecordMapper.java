package com.smartparking.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartparking.record.entity.ParkingRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingRecordMapper extends BaseMapper<ParkingRecord> {
}
