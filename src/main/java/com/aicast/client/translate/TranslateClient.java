package com.aicast.client.translate;

import java.util.List;

public interface TranslateClient {
    /**
     * 원본 텍스트를 대상 언어들로 다국어 번역합니다.
     * 피벗 번역 및 캐싱 전략이 내부에 적용됩니다.
     * @param sourceText 원본 텍스트
     * @param sourceLang 원본 언어 코드
     * @param targetLanguages 번역 대상 언어 코드 목록
     * @return 번역 결과 (언어별 맵핑 포함)
     */
    TranslationResult translate(String sourceText, String sourceLang, List<String> targetLanguages);
}
