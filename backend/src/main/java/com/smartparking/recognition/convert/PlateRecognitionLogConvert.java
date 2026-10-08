package com.smartparking.recognition.convert;

import com.smartparking.recognition.entity.PlateRecognitionLog;
import com.smartparking.recognition.vo.PlateRecognitionLogVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlateRecognitionLogConvert {

    PlateRecognitionLogVO toVO(PlateRecognitionLog log);

    List<PlateRecognitionLogVO> toVOList(List<PlateRecognitionLog> logs);
}
