package com.smartparking.recognition.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.recognition.dto.PlateRecognitionLogQuery;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import com.smartparking.recognition.vo.PlateRecognitionVO;
import org.springframework.web.multipart.MultipartFile;

public interface PlateRecognitionApplicationService {

    PlateRecognitionVO recognize(MultipartFile file);

    PlateRecognitionVO recognizeEntry(MultipartFile file, Long preferredSpaceId);

    PlateRecognitionVO recognizeExit(MultipartFile file);

    PageResponse<PlateRecognitionLogVO> pageLogs(PlateRecognitionLogQuery query);
}
