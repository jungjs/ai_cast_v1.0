package com.aicast.client.nlp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NlpResult {
    private String refinedText;    // 표준어 변환 텍스트
    private String summary;        // 요약 텍스트
    private long processingTimeMs; // 처리 소요시간
    private String status;         // SUCCESS / FAILED
}
