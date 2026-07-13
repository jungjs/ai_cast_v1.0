package com.aicast.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FontManagerTest {

    private FontManager fontManager;

    @BeforeEach
    void setUp() {
        fontManager = new FontManager();
        fontManager.init();
    }

    @Test
    @DisplayName("지원 언어(한국어)에 대한 폰트 로드 검증")
    void getFont_SupportedLanguage_Korean() {
        // When
        Font font = fontManager.getFont("ko", 18);

        // Then
        assertNotNull(font);
        assertEquals(18, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    @DisplayName("지원 언어(영어)에 대한 폰트 로드 검증")
    void getFont_SupportedLanguage_English() {
        // When
        Font font = fontManager.getFont("en", 24);

        // Then
        assertNotNull(font);
        assertEquals(24, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    @DisplayName("미지원 언어 요청 시 OS 기본 폰트(SansSerif)로 폴백 검증")
    void getFont_UnsupportedLanguage_Fallback() {
        // When
        Font font = fontManager.getFont("fr", 15);

        // Then
        assertNotNull(font);
        assertEquals("SansSerif", font.getName());
        assertEquals(15, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    @DisplayName("언어 코드의 앞 2자리만 파싱하여 다국어 폰트 매칭 검증 (예: zh-CN -> zh)")
    void getFont_LanguageSubtagParsing() {
        // When
        Font font = fontManager.getFont("zh-CN", 20);

        // Then
        assertNotNull(font);
        assertEquals(20, font.getSize());
    }
}
