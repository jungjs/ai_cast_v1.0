package com.aicast.client.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String extractedText;      // 추출된 텍스트
    private long processingTimeMs;     // 처리 소요시간
    private String status;             // SUCCESS / FAILED
}
