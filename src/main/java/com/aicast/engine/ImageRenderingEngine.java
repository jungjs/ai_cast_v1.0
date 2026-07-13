package com.aicast.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageRenderingEngine {

    private final CategoryClassifier categoryClassifier;
    private final ColorSchemeMapper colorSchemeMapper;
    private final FontManager fontManager;

    // 생성할 이미지 크기
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int MARGIN = 50;

    /**
     * 번역된 텍스트 목록을 기반으로 PNG 이미지를 생성합니다.
     * @param translations 언어별 번역 텍스트 맵
     * @param originalText 카테고리 분석용 원본(한국어) 텍스트
     * @return PNG 바이트 배열
     */
    public byte[] render(Map<String, String> translations, String originalText) throws Exception {
        
        // 1. 카테고리 및 색상 결정 (F-30, F-31)
        String category = categoryClassifier.classify(originalText);
        Color bgColor = colorSchemeMapper.getBackgroundColor(category);
        Color textColor = colorSchemeMapper.getTextColor(category);

        // 2. 캔버스 준비
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 렌더링 품질 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 배경 채우기
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 상단 카테고리 라벨
        g2d.setColor(Color.RED);
        g2d.setFont(fontManager.getFont("ko", 36));
        g2d.drawString("[" + category + "]", MARGIN, MARGIN + 40);

        // 3. 다국어 텍스트 렌더링 (F-32)
        g2d.setColor(textColor);
        int currentY = 150;
        int maxLines = translations.size();
        int spacePerLang = (HEIGHT - 200) / maxLines;

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String lang = entry.getKey();
            String text = entry.getValue();

            // 폰트 설정
            Font font = fontManager.getFont(lang, 28);
            g2d.setFont(font);

            // 텍스트 줄바꿈 및 출력 로직 (간소화)
            // 실제 환경에서는 FontMetrics를 이용하여 폭을 계산하고 여러 줄로 나누는 로직이 필요함.
            g2d.drawString(String.format("[%s] %s", lang, text), MARGIN, currentY);
            
            currentY += spacePerLang;
        }

        g2d.dispose();

        // 4. PNG 변환
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}
