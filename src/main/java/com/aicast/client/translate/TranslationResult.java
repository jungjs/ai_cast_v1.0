package com.aicast.client.translate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResult {
    private Map<String, String> translations;  // {언어코드: 번역텍스트}
    private long processingTimeMs;
    private String status;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
