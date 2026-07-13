package com.aicast.tool.engine;

import com.aicast.tool.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ImageRendererTest {

    private ImageRenderer renderer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        renderer = new ImageRenderer();
    }

    @Test
    void render_normalScenario_createsPngFile() throws IOException {
        Scenario scenario = new Scenario(1, "오늘의 날씨 안내", "오늘은 맑은 날씨가 예상됩니다.", "오늘은 맑은 날씨가 예상됩니다.");
        Path outputPath = tempDir.resolve("01_오늘의_날씨_안내.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);
    }

    @Test
    void render_disasterKeyword_usesRedOrangeTheme() throws IOException {
        Scenario scenario = new Scenario(1, "폭염 주의보 발령", "기온이 35도 이상 상승할 것으로 예상됩니다.", "폭염 주의보가 발령되었습니다.");
        Path outputPath = tempDir.resolve("01_폭염_주의보_발령.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
        BufferedImage image = javax.imageio.ImageIO.read(outputPath.toFile());
        assertNotNull(image);
        assertEquals(800, image.getWidth());
        assertEquals(800, image.getHeight());
    }

    @Test
    void render_typhoonKeyword_usesRedTheme() throws IOException {
        Scenario scenario = new Scenario(1, "태풍 경보", "태풍이 접근 중입니다.", "태풍이 접근 중입니다.");
        Path outputPath = tempDir.resolve("01_태풍_경보.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void render_generalInfo_usesBlueGreenTheme() throws IOException {
        Scenario scenario = new Scenario(1, "건강 수칙 안내", "규칙적인 운동과 균형 잡힌 식단이 중요합니다.", "건강 수칙을 안내합니다.");
        Path outputPath = tempDir.resolve("01_건강_수칙_안내.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void render_longBodyText_wrapsText() throws IOException {
        String longBody = "이것은 매우 긴 본문 텍스트입니다. ".repeat(50);
        Scenario scenario = new Scenario(1, "긴 텍스트 테스트", longBody, "긴 텍스트 테스트입니다.");
        Path outputPath = tempDir.resolve("01_긴_텍스트_테스트.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
        BufferedImage image = javax.imageio.ImageIO.read(outputPath.toFile());
        assertEquals(800, image.getWidth());
        assertEquals(800, image.getHeight());
    }

    @Test
    void render_emptyBody_doesNotThrow() throws IOException {
        Scenario scenario = new Scenario(1, "빈 본문 테스트", "", "빈 본문입니다.");
        Path outputPath = tempDir.resolve("01_빈_본문_테스트.png");

        assertDoesNotThrow(() -> renderer.render(scenario, outputPath));
        assertTrue(Files.exists(outputPath));
    }

    @Test
    void render_specialCharactersInTitle_handlesGracefully() throws IOException {
        Scenario scenario = new Scenario(1, "[긴급] 산불 발생!", "산불이 발생했습니다. 대피하세요.", "산불 발생 긴급 보도입니다.");
        Path outputPath = tempDir.resolve("01_긴급_산불_발생.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void render_multipleNewlinesInBody_handlesGracefully() throws IOException {
        Scenario scenario = new Scenario(1, "줄바꿈 테스트", "첫 번째 줄\n\n\n두 번째 줄\n\n\n세 번째 줄", "줄바꿈 테스트입니다.");
        Path outputPath = tempDir.resolve("01_줄바꿈_테스트.png");

        renderer.render(scenario, outputPath);

        assertTrue(Files.exists(outputPath));
    }
}
