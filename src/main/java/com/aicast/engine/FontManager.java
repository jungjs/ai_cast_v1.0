package com.aicast.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.Font;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FontManager {

    private final Map<String, Font> fontCache = new ConcurrentHashMap<>();

    // 기본 폰트 크기
    private static final int DEFAULT_FONT_SIZE = 24;

    @PostConstruct
    public void init() {
        // 어플리케이션 시작 시 기본 폰트 로드 시도
        loadFont("ko", "fonts/NotoSansKR-Regular.ttf");
        loadFont("en", "fonts/NotoSans-Regular.ttf");
        loadFont("ja", "fonts/NotoSansJP-Regular.ttf");
        loadFont("zh", "fonts/NotoSansSC-Regular.ttf");
    }

    private void loadFont(String langCode, String fontPath) {
        try {
            ClassPathResource resource = new ClassPathResource(fontPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    fontCache.put(langCode, font.deriveFont(Font.PLAIN, DEFAULT_FONT_SIZE));
                }
            } else {
                log.warn("Font file not found: {}", fontPath);
            }
        } catch (Exception e) {
            log.error("Failed to load font for {}: {}", langCode, e.getMessage());
        }
    }

    /**
     * 언어 코드에 맞는 폰트를 반환합니다.
     * @param langCode 언어 코드 (예: ko, en, ja)
     * @param size 폰트 크기
     * @return 폰트 객체 (없을 경우 시스템 기본 폰트 반환)
     */
    public Font getFont(String langCode, int size) {
        // 중국어의 경우 zh-Hans, zh-Hant 구분을 위해 앞 2자리만 사용
        String baseLang = langCode.length() >= 2 ? langCode.substring(0, 2) : "en";
        
        Font font = fontCache.get(baseLang);
        if (font != null) {
            return font.deriveFont(Font.PLAIN, size);
        }
        
        // 캐시에 없으면 OS 기본 폰트 반환 (대체 수단)
        return new Font("SansSerif", Font.PLAIN, size);
    }
}
