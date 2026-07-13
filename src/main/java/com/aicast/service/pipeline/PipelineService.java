package com.aicast.service.pipeline;

import com.aicast.dto.response.PipelineResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface PipelineService {
    
    /**
     * 오디오 -> 텍스트 변환 -> NLP -> 번역 -> 이미지 -> 스토리지 업로드 (F-03)
     */
    PipelineResponse executeAudio(MultipartFile audioFile, List<String> targetLangs);

    /**
     * 텍스트 -> NLP -> 번역 -> 이미지 -> 스토리지 업로드 (F-04)
     */
    PipelineResponse executeText(String text, List<String> targetLangs);

    /**
     * 이미지 OCR -> 텍스트 변환 -> NLP -> 번역 -> 이미지 -> 스토리지 업로드 (F-05)
     */
    PipelineResponse executeImage(MultipartFile imageFile, List<String> targetLangs);
}
