package com.aicast.tool.engine;

import com.aicast.tool.model.Scenario;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;
import java.util.Map;

import javax.imageio.ImageIO;

@Slf4j
public class ImageRenderer {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final int PADDING = 60;
    private static final int TITLE_AREA_HEIGHT = 200;

    private static final Map<String, Color[]> THEME_MAP = Map.of(
        "폭염", new Color[]{new Color(220, 50, 32), new Color(255, 140, 0)},
        "태풍", new Color[]{new Color(139, 0, 0), new Color(255, 69, 0)},
        "산불", new Color[]{new Color(178, 34, 34), new Color(255, 99, 71)},
        "홍수", new Color[]{new Color(0, 71, 160), new Color(30, 144, 255)},
        "지진", new Color[]{new Color(105, 105, 105), new Color(169, 169, 169)},
        "안전", new Color[]{new Color(0, 100, 0), new Color(34, 139, 34)},
        "건강", new Color[]{new Color(0, 128, 128), new Color(72, 209, 204)},
        "날씨", new Color[]{new Color(30, 144, 255), new Color(135, 206, 250)},
        "정보", new Color[]{new Color(0, 51, 102), new Color(0, 102, 204)}
    );

    private static final Color[] DEFAULT_THEME = new Color[]{new Color(0, 51, 102), new Color(0, 153, 204)};

    public void render(Scenario scenario, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        Color[] theme = detectTheme(scenario.getTitle());
        BufferedImage image = createCardImage(scenario.getTitle(), scenario.getBody(), theme);

        String fileName = outputPath.getFileName().toString();
        String formatName = fileName.substring(fileName.lastIndexOf('.') + 1);
        ImageIO.write(image, formatName, outputPath.toFile());

        log.info("이미지 렌더링 완료: {} ({}x{})", outputPath.getFileName(), WIDTH, HEIGHT);
    }

    private Color[] detectTheme(String title) {
        String lowerTitle = title.toLowerCase();
        for (Map.Entry<String, Color[]> entry : THEME_MAP.entrySet()) {
            if (lowerTitle.contains(entry.getKey())) {
                log.debug("테마 감지: '{}' -> {}", title, entry.getKey());
                return entry.getValue();
            }
        }
        return DEFAULT_THEME;
    }

    private BufferedImage createCardImage(String title, String body, Color[] theme) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        drawBackground(g2d, theme);
        drawTitleArea(g2d, title, theme);
        drawBodyArea(g2d, body);
        drawFooter(g2d, theme);

        g2d.dispose();
        return image;
    }

    private void drawBackground(Graphics2D g2d, Color[] theme) {
        GradientPaint gradient = new GradientPaint(
            0, 0, theme[0],
            WIDTH, HEIGHT, theme[1]
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawTitleArea(Graphics2D g2d, String title, Color[] theme) {
        int titleY = PADDING;

        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.fillRoundRect(PADDING, titleY, WIDTH - (PADDING * 2), TITLE_AREA_HEIGHT - 20, 20, 20);

        Font titleFont = new Font("SansSerif", Font.BOLD, 42);
        g2d.setFont(titleFont);
        g2d.setColor(Color.WHITE);

        drawCenteredText(g2d, title, PADDING, titleY + 20, WIDTH - (PADDING * 2), TITLE_AREA_HEIGHT - 40);
    }

    private void drawBodyArea(Graphics2D g2d, String body) {
        int bodyY = PADDING + TITLE_AREA_HEIGHT + 20;
        int bodyHeight = HEIGHT - (PADDING * 2) - TITLE_AREA_HEIGHT - 80;

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(PADDING, bodyY, WIDTH - (PADDING * 2), bodyHeight, 15, 15);

        Font bodyFont = new Font("SansSerif", Font.PLAIN, 28);
        g2d.setFont(bodyFont);
        g2d.setColor(new Color(33, 33, 33));

        drawWrappedText(g2d, body, PADDING + 30, bodyY + 30, WIDTH - (PADDING * 2) - 60, bodyHeight - 60);
    }

    private void drawFooter(Graphics2D g2d, Color[] theme) {
        int footerY = HEIGHT - PADDING;

        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 16));
        g2d.drawString("AI Cast News", PADDING, footerY);

        String copyright = "© 2026 AI Cast";
        FontMetrics fm = g2d.getFontMetrics();
        int copyrightWidth = fm.stringWidth(copyright);
        g2d.drawString(copyright, WIDTH - PADDING - copyrightWidth, footerY);
    }

    private void drawCenteredText(Graphics2D g2d, String text, int x, int y, int maxWidth, int maxHeight) {
        FontRenderContext frc = g2d.getFontRenderContext();
        String[] lines = wrapText(text, g2d.getFont(), frc, maxWidth);

        int totalHeight = 0;
        int lineHeight = g2d.getFontMetrics().getHeight();
        for (String line : lines) {
            totalHeight += lineHeight;
        }

        int currentY = y + (maxHeight - totalHeight) / 2 + g2d.getFontMetrics().getAscent();

        for (String line : lines) {
            Rectangle2D bounds = g2d.getFont().getStringBounds(line, frc);
            int lineWidth = (int) bounds.getWidth();
            int currentX = x + (maxWidth - lineWidth) / 2;
            g2d.drawString(line, currentX, currentY);
            currentY += lineHeight;
        }
    }

    private void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth, int maxHeight) {
        FontRenderContext frc = g2d.getFontRenderContext();
        String[] lines = wrapText(text, g2d.getFont(), frc, maxWidth);

        int lineHeight = g2d.getFontMetrics().getHeight();
        int currentY = y + g2d.getFontMetrics().getAscent();

        for (String line : lines) {
            if (currentY - y + lineHeight > maxHeight) {
                g2d.drawString("...", x, currentY);
                break;
            }
            g2d.drawString(line, x, currentY);
            currentY += lineHeight;
        }
    }

    private String[] wrapText(String text, Font font, FontRenderContext frc, int maxWidth) {
        String[] paragraphs = text.split("\\n");
        var result = new java.util.ArrayList<String>();

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                result.add("");
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                Rectangle2D bounds = font.getStringBounds(testLine, frc);

                if (bounds.getWidth() > maxWidth && currentLine.length() > 0) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
        }

        return result.toArray(new String[0]);
    }
}
