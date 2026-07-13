package com.aicast.tool;

import com.aicast.tool.client.TtsClient;
import com.aicast.tool.engine.ImageRenderer;
import com.aicast.tool.model.Scenario;
import com.aicast.tool.parser.MarkdownScenarioParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

@Slf4j
public class TestDataGenerator {

    private final Path outputDir;
    private final Path markdownFile;
    private final TtsClient ttsClient;
    private final ImageRenderer imageRenderer;
    private final MarkdownScenarioParser parser;

    public TestDataGenerator(Properties config) throws IOException {
        this.outputDir = Paths.get(config.getProperty("output.dir", "docs/80_RawData/testdata"));
        this.markdownFile = Paths.get(config.getProperty("markdown.file", "scenario.md"));
        this.ttsClient = new TtsClient(
            config.getProperty("azure.speech.key", ""),
            config.getProperty("azure.speech.region", "koreacentral")
        );
        this.imageRenderer = new ImageRenderer();
        this.parser = new MarkdownScenarioParser();

        Files.createDirectories(outputDir);
        log.info("출력 디렉토리: {}", outputDir.toAbsolutePath());
    }

    public static void main(String[] args) {
        try {
            Properties config = loadConfig(args);
            TestDataGenerator generator = new TestDataGenerator(config);
            generator.run();
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            System.exit(1);
        }
    }

    public void run() throws IOException {
        log.info("=== 테스트 데이터 생성 시작 ===");

        List<Scenario> scenarios = parser.parse(markdownFile);
        log.info("파싱된 시나리오 수: {}", scenarios.size());

        int successCount = 0;
        int failCount = 0;

        for (Scenario scenario : scenarios) {
            try {
                processScenario(scenario);
                successCount++;
            } catch (Exception e) {
                log.error("시나리오 처리 실패: index={}, title={}", scenario.getIndex(), scenario.getTitle(), e);
                failCount++;
            }
        }

        log.info("=== 테스트 데이터 생성 완료 ===");
        log.info("성공: {}, 실패: {}, 총: {}", successCount, failCount, scenarios.size());
    }

    private void processScenario(Scenario scenario) throws IOException {
        String safeFileName = scenario.getSafeFileName();
        log.info("[{}/{}] 처리 중: {}", scenario.getIndex(), safeFileName);

        Path wavPath = outputDir.resolve(safeFileName + ".wav");
        Path pngPath = outputDir.resolve(safeFileName + ".png");

        if (scenario.getBody() != null && !scenario.getBody().isEmpty()) {
            log.info("  TTS 생성: {}", wavPath.getFileName());
            ttsClient.synthesizeToWav(scenario.getBody(), wavPath);
        } else {
            log.info("  TTS 체크 포인트 없음, 건너뜀");
        }

        log.info("  이미지 렌더링: {}", pngPath.getFileName());
        imageRenderer.render(scenario, pngPath);

        log.info("  완료: {}", safeFileName);
    }

    private static Properties loadConfig(String[] args) throws IOException {
        Properties config = new Properties();

        Path configPath = Paths.get("config.properties");
        if (Files.exists(configPath)) {
            try (var reader = Files.newBufferedReader(configPath)) {
                config.load(reader);
                log.info("설정 파일 로드: {}", configPath.toAbsolutePath());
            }
        }

        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                config.setProperty(key, args[i + 1]);
            }
        }

        return config;
    }

    public int generateAll() throws IOException {
        List<Scenario> scenarios = parser.parse(markdownFile);
        int count = 0;

        for (Scenario scenario : scenarios) {
            processScenario(scenario);
            count++;
        }

        return count;
    }
}
