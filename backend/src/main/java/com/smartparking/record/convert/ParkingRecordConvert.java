package com.smartparking.record.convert;

import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.vo.ParkingRecordVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParkingRecordConvert {

    ParkingRecordVO toVO(ParkingRecord parkingRecord);

    List<ParkingRecordVO> toVOList(List<ParkingRecord> parkingRecords);
}
