package com.aicast.client.nlp;

public interface NlpClient {
    /**
     * 사투리 정제 및 요약을 한 번에 수행합니다 (FAST_MODE).
     * @param rawText 원본 텍스트
     * @return 정제 및 요약 결과
     */
    NlpResult processText(String rawText);

    /**
     * 사투리만 정제합니다.
     * @param rawText 원본 텍스트
     * @return 정제된 텍스트
     */
    String refineText(String rawText);

    /**
     * 텍스트를 요약합니다.
     * @param refinedText 정제된 텍스트
     * @return 요약된 텍스트
     */
    String summarizeText(String refinedText);
}
