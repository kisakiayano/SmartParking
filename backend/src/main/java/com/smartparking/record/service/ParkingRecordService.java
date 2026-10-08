package com.smartparking.record.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.record.dto.ManualEntryRequest;
import com.smartparking.record.dto.ManualExitRequest;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.vo.ParkingRecordVO;

public interface ParkingRecordService {

    ParkingRecordVO manualEntry(ManualEntryRequest request);

    ParkingRecordVO manualExit(ManualExitRequest request);

    ParkingRecordVO autoEntry(String plateNumber, Long preferredSpaceId);

    ParkingRecordVO autoExit(String plateNumber);

    ParkingRecordVO getById(Long recordId);

    ParkingRecordVO getMineById(Long currentUserId, Long recordId);

    PageResponse<ParkingRecordVO> pageMine(Long currentUserId, ParkingRecordPageQuery query);

    PageResponse<ParkingRecordVO> pageForAdmin(ParkingRecordPageQuery query);
}
