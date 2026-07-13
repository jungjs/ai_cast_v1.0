package com.aicast.engine;

import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.Map;

@Component
public class ColorSchemeMapper {
    
    private static final Map<String, Color> BACKGROUND_COLORS = Map.of(
        "DISASTER", new Color(255, 200, 200), // 연한 빨강
        "NOTICE", new Color(200, 200, 255),   // 연한 파랑
        "INFO", new Color(220, 255, 220)      // 연한 초록
    );

    public Color getBackgroundColor(String category) {
        if (category == null) {
            return Color.WHITE;
        }
        return BACKGROUND_COLORS.getOrDefault(category, Color.WHITE);
    }
    
    public Color getTextColor(String category) {
        return Color.BLACK; // 공통 텍스트 색상
    }
}
