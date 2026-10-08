package com.smartparking.vehicle.convert;

import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.vo.VehicleVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleConvert {

    VehicleVO toVO(Vehicle vehicle);

    List<VehicleVO> toVOList(List<Vehicle> vehicles);
}
