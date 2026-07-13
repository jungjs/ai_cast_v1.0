package com.aicast.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorSchemeMapperTest {

    private final ColorSchemeMapper colorSchemeMapper = new ColorSchemeMapper();

    @Test
    @DisplayName("카테고리별 배경 색상 맵 매핑 검증")
    void getBackgroundColor_CategoryMapping() {
        // Given & When & Then
        assertEquals(new Color(255, 200, 200), colorSchemeMapper.getBackgroundColor("DISASTER"));
        assertEquals(new Color(200, 200, 255), colorSchemeMapper.getBackgroundColor("NOTICE"));
        assertEquals(new Color(220, 255, 220), colorSchemeMapper.getBackgroundColor("INFO"));
    }

    @Test
    @DisplayName("정의되지 않은 카테고리 입력 시 기본 색상(WHITE) 반환 검증")
    void getBackgroundColor_Fallback() {
        // Given & When & Then
        assertEquals(Color.WHITE, colorSchemeMapper.getBackgroundColor("UNKNOWN"));
        assertEquals(Color.WHITE, colorSchemeMapper.getBackgroundColor(null));
        assertEquals(Color.WHITE, colorSchemeMapper.getBackgroundColor(""));
    }

    @Test
    @DisplayName("텍스트 색상은 항상 BLACK 반환 검증")
    void getTextColor_AlwaysBlack() {
        // Given & When & Then
        assertEquals(Color.BLACK, colorSchemeMapper.getTextColor("DISASTER"));
        assertEquals(Color.BLACK, colorSchemeMapper.getTextColor("NOTICE"));
        assertEquals(Color.BLACK, colorSchemeMapper.getTextColor("INFO"));
        assertEquals(Color.BLACK, colorSchemeMapper.getTextColor("UNKNOWN"));
        assertEquals(Color.BLACK, colorSchemeMapper.getTextColor(null));
    }
}
