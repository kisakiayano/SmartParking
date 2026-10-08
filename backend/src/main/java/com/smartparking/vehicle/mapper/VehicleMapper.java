package com.smartparking.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartparking.vehicle.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
}
