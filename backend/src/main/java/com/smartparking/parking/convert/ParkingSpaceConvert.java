package com.smartparking.parking.convert;

import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParkingSpaceConvert {

    ParkingSpaceVO toVO(ParkingSpace parkingSpace);

    ParkingSpaceStatusVO toStatusVO(ParkingSpace parkingSpace);

    List<ParkingSpaceVO> toVOList(List<ParkingSpace> parkingSpaces);
}
