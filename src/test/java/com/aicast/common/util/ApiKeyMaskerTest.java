package com.aicast.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyMaskerTest {

    @Test
    @DisplayName("8자 초과 표준 API Key 마스킹 검증")
    void mask_StandardKey() {
        // Given
        String apiKey = "1234567890abcdef";

        // When
        String result = ApiKeyMasker.mask(apiKey);

        // Then
        assertEquals("1234***cdef", result);
    }

    @Test
    @DisplayName("8자 이하 짧은 API Key 마스킹 검증")
    void mask_ShortKey() {
        // Given
        String apiKey1 = "12345678";
        String apiKey2 = "abc";

        // When & Then
        assertEquals("***", ApiKeyMasker.mask(apiKey1));
        assertEquals("***", ApiKeyMasker.mask(apiKey2));
    }

    @Test
    @DisplayName("Null 또는 빈 값 API Key 마스킹 검증")
    void mask_NullOrEmpty() {
        // When & Then
        assertEquals("***", ApiKeyMasker.mask(null));
        assertEquals("***", ApiKeyMasker.mask(""));
        assertEquals("***", ApiKeyMasker.mask("   "));
    }
}
