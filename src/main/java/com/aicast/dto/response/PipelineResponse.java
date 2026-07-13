package com.aicast.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineResponse {
    private String correlationId;
    private String status;           // SUCCESS / FAILED
    
    private String originalText;     // 원본 텍스트 (STT/OCR/입력)
    private String refinedText;      // NLP 정제 텍스트
    private String summary;          // NLP 요약 텍스트
    
    private Map<String, String> translations; // 언어별 번역본
    
    private String imageUrl;         // 최종 생성된 이미지 URL
    private String errorMessage;     // 에러 발생 시
    
    private Map<String, Long> processingTimesMs; // 단계별 소요시간 (모니터링용)
}
