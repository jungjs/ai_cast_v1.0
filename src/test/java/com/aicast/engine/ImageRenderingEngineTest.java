package com.aicast.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Color;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageRenderingEngineTest {

    @Mock private CategoryClassifier categoryClassifier;
    @Mock private ColorSchemeMapper colorSchemeMapper;
    @Mock private FontManager fontManager;

    @InjectMocks
    private ImageRenderingEngine engine;

    @Test
    @DisplayName("이미지 렌더링 정상 생성 및 반환 검증")
    void renderImage_Success(@TempDir Path tempDir) throws Exception {
        // Given
        String originalText = "이것은 테스트 원문입니다.";
        Map<String, String> translations = new HashMap<>();
        translations.put("en", "This is a test translation.");
        translations.put("ko", "이것은 테스트 번역입니다.");

        when(categoryClassifier.classify(originalText)).thenReturn("NOTICE");
        when(colorSchemeMapper.getBackgroundColor("NOTICE")).thenReturn(new Color(200, 200, 255));
        when(colorSchemeMapper.getTextColor("NOTICE")).thenReturn(Color.BLACK);
        
        Font mockFont = new Font("SansSerif", Font.PLAIN, 28);
        when(fontManager.getFont(anyString(), anyInt())).thenReturn(mockFont);

        // When
        byte[] imageBytes = engine.render(translations, originalText);

        // Then
        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0);

        // 물리적 파일 생성 확인 (Optional Test)
        Path testFile = tempDir.resolve("test_output.png");
        Files.write(testFile, imageBytes);
        assertTrue(Files.exists(testFile));
        assertTrue(Files.size(testFile) > 0);
        
        verify(categoryClassifier, times(1)).classify(originalText);
        verify(colorSchemeMapper, times(1)).getBackgroundColor("NOTICE");
        verify(colorSchemeMapper, times(1)).getTextColor("NOTICE");
    }

    @Test
    @DisplayName("극단적으로 긴 텍스트 입력 시 폰트 크기 자동 조절 및 레이아웃 깨짐 방지 검증")
    void renderImage_LongText(@TempDir Path tempDir) throws Exception {
        // Given
        String longText = "A".repeat(10000); // extremely long text
        Map<String, String> translations = new HashMap<>();
        translations.put("en", longText);
        translations.put("ko", longText);

        when(categoryClassifier.classify(longText)).thenReturn("INFO");
        when(colorSchemeMapper.getBackgroundColor("INFO")).thenReturn(new Color(220, 255, 220));
        when(colorSchemeMapper.getTextColor("INFO")).thenReturn(Color.BLACK);
        
        Font mockFont = new Font("SansSerif", Font.PLAIN, 28);
        when(fontManager.getFont(anyString(), anyInt())).thenReturn(mockFont);

        // When
        byte[] imageBytes = engine.render(translations, longText);

        // Then
        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0);
        
        // Verify no exception and image created
        Path testFile = tempDir.resolve("test_long_text.png");
        Files.write(testFile, imageBytes);
        assertTrue(Files.exists(testFile));
        assertTrue(Files.size(testFile) > 0);
    }

    @Test
    @DisplayName("정의되지 않은 카테고리 입력 시 기본 테마 배색으로 카드 생성 검증")
    void renderImage_InvalidCategory(@TempDir Path tempDir) throws Exception {
        // Given
        String originalText = "알 수 없는 카테고리 텍스트";
        Map<String, String> translations = new HashMap<>();
        translations.put("en", "Unknown category text");

        when(categoryClassifier.classify(originalText)).thenReturn("UNKNOWN");
        when(colorSchemeMapper.getBackgroundColor("UNKNOWN")).thenReturn(Color.WHITE);
        when(colorSchemeMapper.getTextColor("UNKNOWN")).thenReturn(Color.BLACK);
        
        Font mockFont = new Font("SansSerif", Font.PLAIN, 28);
        when(fontManager.getFont(anyString(), anyInt())).thenReturn(mockFont);

        // When
        byte[] imageBytes = engine.render(translations, originalText);

        // Then
        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0);
        
        // Verify image created with default white background
        Path testFile = tempDir.resolve("test_invalid_category.png");
        Files.write(testFile, imageBytes);
        assertTrue(Files.exists(testFile));
        assertTrue(Files.size(testFile) > 0);
        
        verify(categoryClassifier, times(1)).classify(originalText);
        verify(colorSchemeMapper, times(1)).getBackgroundColor("UNKNOWN");
        verify(colorSchemeMapper, times(1)).getTextColor("UNKNOWN");
    }
}