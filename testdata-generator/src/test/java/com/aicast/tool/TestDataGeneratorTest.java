package com.aicast.tool;

import com.aicast.tool.model.Scenario;
import com.aicast.tool.parser.MarkdownScenarioParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class TestDataGeneratorTest {

    private MarkdownScenarioParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new MarkdownScenarioParser();
    }

    @Test
    void parser_parseMarkdown_returnsScenarios() throws IOException {
        String markdown = """
            # 테스트 시나리오
            
            [제목]
            오늘의 날씨 안내
            
            [본문]
            오늘은 맑은 날씨가 예상됩니다.
            
            [TTS 체크 포인트]
            오늘은 맑은 날씨가 예상됩니다.
            
            ---
            
            [제목]
            폭염 주의보
            
            [본문]
            기온이 35도 이상 상승할 것으로 예상됩니다.
            
            [TTS 체크 포인트]
            폭염 주의보가 발령되었습니다.
            """;

        Path mdFile = tempDir.resolve("test_scenario.md");
        Files.writeString(mdFile, markdown);

        List<Scenario> scenarios = parser.parse(mdFile);

        assertEquals(2, scenarios.size());
        assertEquals("오늘의 날씨 안내", scenarios.get(0).getTitle());
        assertEquals("폭염 주의보", scenarios.get(1).getTitle());
    }

    @Test
    void scenario_getSafeFileName_removesSpecialChars() {
        Scenario scenario = new Scenario(1, "[긴급] 산불 발생!", "내용", "TTS");
        String safeName = scenario.getSafeFileName();

        assertFalse(safeName.contains("["));
        assertFalse(safeName.contains("]"));
        assertFalse(safeName.contains("!"));
        assertTrue(safeName.startsWith("01_"));
    }

    @Test
    void parser_parseWithEmptyContent_returnsEmptyList() {
        List<Scenario> scenarios = parser.parse("");
        assertTrue(scenarios.isEmpty());
    }

    @Test
    void parser_parseWithOnlyTitle_returnsEmptyList() {
        String markdown = """
            [제목]
            제목만 있는 경우
            """;

        List<Scenario> scenarios = parser.parse(markdown);
        assertTrue(scenarios.isEmpty());
    }

    @Test
    void parser_parseMultipleScenarios_returnsCorrectCount() {
        String markdown = """
            [제목]
            첫 번째 제목
            
            [본문]
            첫 번째 본문
            
            [TTS 체크 포인트]
            첫 번째 TTS
            
            ---
            
            [제목]
            두 번째 제목
            
            [본문]
            두 번째 본문
            
            [TTS 체크 포인트]
            두 번째 TTS
            
            ---
            
            [제목]
            세 번째 제목
            
            [본문]
            세 번째 본문
            
            [TTS 체크 포인트]
            세 번째 TTS
            """;

        List<Scenario> scenarios = parser.parse(markdown);
        assertEquals(3, scenarios.size());
    }

    @Test
    void testDataGenerator_loadConfig_defaultValues() throws IOException {
        Properties config = new Properties();
        config.setProperty("output.dir", tempDir.toString());
        config.setProperty("markdown.file", "test.md");

        assertEquals(tempDir.toString(), config.getProperty("output.dir"));
        assertEquals("test.md", config.getProperty("markdown.file"));
        assertEquals("koreacentral", config.getProperty("azure.speech.region", "koreacentral"));
    }
}
