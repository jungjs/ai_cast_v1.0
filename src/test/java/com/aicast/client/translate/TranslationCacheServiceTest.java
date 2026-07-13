package com.aicast.client.translate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TranslationCacheServiceTest {

    private TranslationCacheService translationCacheService;

    @BeforeEach
    void setUp() {
        translationCacheService = new TranslationCacheService(10, 1);
    }

    @Test
    @DisplayName("캐시에 존재하지 않는 번역 요청 시 null 반환 검증 (Cache Miss)")
    void get_CacheMiss() {
        // When
        String cachedValue = translationCacheService.get("Hello", "KO");

        // Then
        assertNull(cachedValue);
    }

    @Test
    @DisplayName("캐시에 번역 데이터 저장 후 정상 조회 검증 (Cache Hit)")
    void get_CacheHit() {
        // Given
        String sourceText = "Hello";
        String targetLang = "KO";
        String translatedText = "안녕하세요";

        // When
        translationCacheService.put(sourceText, targetLang, translatedText);
        String cachedValue = translationCacheService.get(sourceText, targetLang);

        // Then
        assertEquals(translatedText, cachedValue);
    }

    @Test
    @DisplayName("동일 텍스트라도 대상 언어가 다르면 캐시 키가 다르게 구분되는지 검증")
    void get_DifferentTargetLanguage() {
        // Given
        String sourceText = "Hello";
        translationCacheService.put(sourceText, "KO", "안녕하세요");
        translationCacheService.put(sourceText, "JA", "こんにちは");

        // When
        String koResult = translationCacheService.get(sourceText, "KO");
        String jaResult = translationCacheService.get(sourceText, "JA");

        // Then
        assertEquals("안녕하세요", koResult);
        assertEquals("こんにちは", jaResult);
    }
}
