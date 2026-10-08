package com.smartparking.recognition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.recognition.convert.PlateRecognitionLogConvert;
import com.smartparking.recognition.dto.PlateRecognitionLogQuery;
import com.smartparking.recognition.entity.PlateRecognitionLog;
import com.smartparking.recognition.enums.RecognitionCameraType;
import com.smartparking.recognition.enums.RecognitionResult;
import com.smartparking.recognition.mapper.PlateRecognitionLogMapper;
import com.smartparking.recognition.model.PlateRecognitionResult;
import com.smartparking.recognition.service.PlateRecognitionApplicationService;
import com.smartparking.recognition.service.PlateRecognitionService;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import com.smartparking.recognition.vo.PlateRecognitionVO;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class PlateRecognitionApplicationServiceImpl implements PlateRecognitionApplicationService {

    private final PlateRecognitionService plateRecognitionService;
    private final ParkingRecordService parkingRecordService;
    private final PlateRecognitionLogMapper logMapper;
    private final PlateRecognitionLogConvert logConvert;

    public PlateRecognitionApplicationServiceImpl(
            PlateRecognitionService plateRecognitionService,
            ParkingRecordService parkingRecordService,
            PlateRecognitionLogMapper logMapper,
            PlateRecognitionLogConvert logConvert
    ) {
        this.plateRecognitionService = plateRecognitionService;
        this.parkingRecordService = parkingRecordService;
        this.logMapper = logMapper;
        this.logConvert = logConvert;
    }

    @Override
    public PlateRecognitionVO recognize(MultipartFile file) {
        PlateRecognitionResult result = recognizeImage(file);
        RecognitionResult logResult = resolveLogResult(result);
        PlateRecognitionLog log = insertLog(result, RecognitionCameraType.UPLOAD, logResult, fileName(file), null, null, null);
        if (!result.recognized()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, result.errorMessage());
        }
        return toVO(result, log, null);
    }

    @Override
    public PlateRecognitionVO recognizeEntry(MultipartFile file, Long preferredSpaceId) {
        PlateRecognitionResult result = recognizeImage(file);
        if (!result.recognized()) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.ENTRY, RecognitionResult.FAILED, fileName(file), null, result.errorCode(), result.errorMessage());
            throw new BusinessException(ResultCode.BAD_REQUEST, log.getErrorMessage());
        }
        if (!result.confidenceQualified()) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.ENTRY, RecognitionResult.LOW_CONFIDENCE, fileName(file), null, "LOW_CONFIDENCE", "plate recognition confidence is too low");
            throw new BusinessException(ResultCode.BAD_REQUEST, log.getErrorMessage());
        }
        try {
            ParkingRecordVO parkingRecord = parkingRecordService.autoEntry(result.plateNumber(), preferredSpaceId);
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.ENTRY, RecognitionResult.SUCCESS, fileName(file), parkingRecord.recordId(), null, null);
            return toVO(result, log, parkingRecord);
        } catch (BusinessException ex) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.ENTRY, RecognitionResult.FAILED, fileName(file), null, ex.getResultCode().name(), ex.getMessage());
            throw new BusinessException(ex.getResultCode(), log.getErrorMessage());
        }
    }

    @Override
    public PlateRecognitionVO recognizeExit(MultipartFile file) {
        PlateRecognitionResult result = recognizeImage(file);
        if (!result.recognized()) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.EXIT, RecognitionResult.FAILED, fileName(file), null, result.errorCode(), result.errorMessage());
            throw new BusinessException(ResultCode.BAD_REQUEST, log.getErrorMessage());
        }
        if (!result.confidenceQualified()) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.EXIT, RecognitionResult.LOW_CONFIDENCE, fileName(file), null, "LOW_CONFIDENCE", "plate recognition confidence is too low");
            throw new BusinessException(ResultCode.BAD_REQUEST, log.getErrorMessage());
        }
        try {
            ParkingRecordVO parkingRecord = parkingRecordService.autoExit(result.plateNumber());
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.EXIT, RecognitionResult.SUCCESS, fileName(file), parkingRecord.recordId(), null, null);
            return toVO(result, log, parkingRecord);
        } catch (BusinessException ex) {
            PlateRecognitionLog log = insertLog(result, RecognitionCameraType.EXIT, RecognitionResult.FAILED, fileName(file), null, ex.getResultCode().name(), ex.getMessage());
            throw new BusinessException(ex.getResultCode(), log.getErrorMessage());
        }
    }

    @Override
    public PageResponse<PlateRecognitionLogVO> pageLogs(PlateRecognitionLogQuery query) {
        PlateRecognitionLogQuery normalizedQuery = query == null
                ? new PlateRecognitionLogQuery(null, null, null, null, null, null, null, null)
                : query;
        LambdaQueryWrapper<PlateRecognitionLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(normalizedQuery.plateNumber())) {
            wrapper.eq(PlateRecognitionLog::getPlateNumber, normalizedQuery.plateNumber().trim().toUpperCase());
        }
        if (StringUtils.hasText(normalizedQuery.cameraType())) {
            wrapper.eq(PlateRecognitionLog::getCameraType, normalizedQuery.cameraType());
        }
        if (StringUtils.hasText(normalizedQuery.provider())) {
            wrapper.eq(PlateRecognitionLog::getProvider, normalizedQuery.provider().trim().toUpperCase());
        }
        if (StringUtils.hasText(normalizedQuery.result())) {
            wrapper.eq(PlateRecognitionLog::getResult, normalizedQuery.result());
        }
        if (normalizedQuery.startTime() != null) {
            wrapper.ge(PlateRecognitionLog::getRecognitionTime, normalizedQuery.startTime());
        }
        if (normalizedQuery.endTime() != null) {
            wrapper.le(PlateRecognitionLog::getRecognitionTime, normalizedQuery.endTime());
        }
        wrapper.orderByDesc(PlateRecognitionLog::getRecognitionTime);
        IPage<PlateRecognitionLog> page = logMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                logConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private PlateRecognitionResult recognizeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "recognition image is required");
        }
        try {
            return plateRecognitionService.recognize(file.getBytes());
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "failed to read recognition image");
        }
    }

    private RecognitionResult resolveLogResult(PlateRecognitionResult result) {
        if (!result.recognized()) {
            return RecognitionResult.FAILED;
        }
        if (!result.confidenceQualified()) {
            return RecognitionResult.LOW_CONFIDENCE;
        }
        return RecognitionResult.SUCCESS;
    }

    private PlateRecognitionLog insertLog(
            PlateRecognitionResult result,
            RecognitionCameraType cameraType,
            RecognitionResult recognitionResult,
            String imageUrl,
            Long recordId,
            String errorCode,
            String errorMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        PlateRecognitionLog log = new PlateRecognitionLog();
        log.setRecordId(recordId);
        log.setPlateNumber(result.plateNumber());
        log.setPlateColor(result.plateColor());
        log.setConfidence(result.confidence());
        log.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : "uploaded-image");
        log.setRecognitionTime(now);
        log.setCameraType(cameraType.name());
        log.setProvider(result.provider());
        log.setProviderLogId(result.providerLogId());
        log.setResult(recognitionResult.name());
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        log.setCreateTime(now);
        logMapper.insert(log);
        return log;
    }

    private PlateRecognitionVO toVO(PlateRecognitionResult result, PlateRecognitionLog log, ParkingRecordVO parkingRecord) {
        return new PlateRecognitionVO(
                result.plateNumber(),
                result.plateColor(),
                result.confidence(),
                result.provider(),
                result.providerLogId(),
                log.getId(),
                parkingRecord
        );
    }

    private String fileName(MultipartFile file) {
        return file == null ? null : file.getOriginalFilename();
    }
}
