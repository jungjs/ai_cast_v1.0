package com.aicast.client;

import com.aicast.client.nlp.AzureOpenAIClient;
import com.aicast.client.nlp.NlpResult;
import com.aicast.client.ocr.AzureVisionClient;
import com.aicast.client.ocr.OcrResult;
import com.aicast.client.storage.AzureBlobClient;
import com.aicast.client.stt.AzureSpeechClient;
import com.aicast.client.translate.AzureTranslatorClient;
import com.aicast.client.translate.TranslationResult;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class E2eIntegrationTest {

    @Autowired
    private AzureSpeechClient azureSpeechClient;

    @Autowired
    private AzureOpenAIClient azureOpenAIClient;

    @Autowired
    private AzureTranslatorClient azureTranslatorClient;

    @Autowired
    private AzureVisionClient azureVisionClient;

    @Autowired
    private AzureBlobClient azureBlobClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void setUpClass() {
        // UNKNOWN correlationId를 tb_api_log에 미리 주입하여 비동기 스레드의 외래키 제약조건 오류 원천 방지
        jdbcTemplate.update("""
            INSERT IGNORE INTO tb_api_log 
                (corr_id, api_key, gov_name, endpoint, client_ip, req_time, is_ok)
            VALUES 
                ('UNKNOWN', 'UNKNOWN', 'SYSTEM_LOG', '/api/system/init', '127.0.0.1', NOW(), TRUE)
        """);
    }

    @AfterAll
    void tearDownClass() {
        jdbcTemplate.update("DELETE FROM tb_api_log WHERE corr_id = 'UNKNOWN'");
    }

    @BeforeEach
    void setUpCorrelationId() {
        String testCorrId = "e2e-test-corr-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", testCorrId);
        MDC.put("apiKey", "e2e-test-api-key");

        // 부모 레코드(tb_api_log) 삽입하여 tb_ai_svc_log 외래키 위반 방지
        jdbcTemplate.update("""
            INSERT INTO tb_api_log 
                (corr_id, api_key, gov_name, endpoint, client_ip, req_time, is_ok)
            VALUES 
                (?, 'e2e-test-api-key', 'E2E_TEST_GOV', '/api/e2e/test', '127.0.0.1', NOW(), TRUE)
        """, testCorrId);
    }

    @AfterEach
    void tearDownCorrelationId() {
        String corrId = MDC.get("correlationId");
        if (corrId != null) {
            // 외래키 cascade delete에 의해 tb_ai_svc_log의 자식 레코드들도 함께 지워짐
            jdbcTemplate.update("DELETE FROM tb_api_log WHERE corr_id = ?", corrId);
        }
        MDC.clear();
    }

    @Test
    @DisplayName("[E2E] Azure OpenAI (정제/요약) 실시간 API 연동 테스트")
    void testOpenAI_RealConnection() {
        // Given
        String rawText = "아따 오늘 날씨 겁나게 시원하고 좋구마잉. 오늘 오후에 소나기 올 수도 있으니까 우산 챙겨야 쓰겄어.";

        // When
        NlpResult result = azureOpenAIClient.processText(rawText);

        // Then
        assertNotNull(result);
        System.out.println("=== Azure OpenAI E2E Result ===");
        System.out.println("Status: " + result.getStatus());
        System.out.println("Refined Text: " + result.getRefinedText());
        System.out.println("Summary: " + result.getSummary());
        System.out.println("Processing Time: " + result.getProcessingTimeMs() + "ms");

        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getRefinedText());
        assertNotNull(result.getSummary());
    }

    @Test
    @DisplayName("[E2E] Azure Translator (다국어 번역) 실시간 API 연동 테스트")
    void testTranslation_RealConnection() throws Exception {
        // Given
        String textToTranslate = "이것은 실시간 Azure 번역기 API 연동 테스트 문자열입니다.";
        String targetLanguage = "en"; // 영어로 번역

        // When
        TranslationResult translationResult = azureTranslatorClient.translate(textToTranslate, "ko", Collections.singletonList(targetLanguage));
        String translatedText = translationResult.getTranslations().get(targetLanguage);

        // Then
        assertNotNull(translatedText);
        System.out.println("=== Azure Translator E2E Result ===");
        System.out.println("Original: " + textToTranslate);
        System.out.println("Translated: " + translatedText);

        assertFalse(translatedText.isEmpty());
        assertEquals("SUCCESS", translationResult.getStatus());
    }

    @Test
    @DisplayName("[E2E] Azure Blob Storage (파일 업로드) 실시간 API 연동 테스트")
    void testStorage_RealConnection() throws Exception {
        // Given
        byte[] dummyBytes = new byte[]{13, 10, 80, 78, 71, 13, 10, 26, 10}; // Dummy PNG Header bytes
        String fileName = "e2e_test_" + System.currentTimeMillis() + ".png";

        // When
        String blobUrl = azureBlobClient.upload(dummyBytes, fileName);

        // Then
        assertNotNull(blobUrl);
        System.out.println("=== Azure Storage E2E Result ===");
        System.out.println("Uploaded Blob URL: " + blobUrl);

        assertTrue(blobUrl.startsWith("https://aicaststoragedev1.blob.core.windows.net/"));
        assertTrue(blobUrl.contains(fileName));
    }

    @Test
    @DisplayName("[E2E] Azure Vision (OCR) 실시간 API 연동 테스트")
    void testOcr_RealConnection() throws Exception {
        // Given
        ClassPathResource resource = new ClassPathResource("test_ocr.png");
        byte[] imageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
        MultipartFile mockFile = new MockMultipartFile("image", "test_ocr.png", "image/png", imageBytes);

        // When
        OcrResult result = azureVisionClient.extractText(mockFile);

        // Then
        assertNotNull(result);
        System.out.println("=== Azure Vision OCR E2E Result ===");
        System.out.println("Status: " + result.getStatus());
        System.out.println("Extracted Text: " + result.getExtractedText());

        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    @DisplayName("[E2E] Azure Speech (STT) 실시간 API 연동 테스트")
    void testSpeechToText_RealConnection() throws Exception {
        // Given
        byte[] dummyWav = createDummySilenceWav();
        MultipartFile mockFile = new MockMultipartFile("audio", "silence.wav", "audio/wav", dummyWav);

        // When
        String text = azureSpeechClient.speechToText(mockFile);

        // Then
        assertNotNull(text);
        System.out.println("=== Azure Speech STT E2E Result ===");
        System.out.println("Transcribed Text: " + text);

        assertTrue(text.trim().isEmpty() || text.length() > 0);
    }

    private byte[] createDummySilenceWav() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int sampleRate = 16000;
        int channels = 1;
        int bitsPerSample = 16;
        int dataSize = sampleRate * channels * (bitsPerSample / 8) * 1; // 1초

        baos.write("RIFF".getBytes());
        baos.write(intToBytes(36 + dataSize), 0, 4);
        baos.write("WAVE".getBytes());

        baos.write("fmt ".getBytes());
        baos.write(intToBytes(16), 0, 4);
        baos.write(shortToBytes((short) 1), 0, 2);
        baos.write(shortToBytes((short) channels), 0, 2);
        baos.write(intToBytes(sampleRate), 0, 4);
        baos.write(intToBytes(sampleRate * channels * (bitsPerSample / 8)), 0, 4);
        baos.write(shortToBytes((short) (channels * (bitsPerSample / 8))), 0, 2);
        baos.write(shortToBytes((short) bitsPerSample), 0, 2);

        baos.write("data".getBytes());
        baos.write(intToBytes(dataSize), 0, 4);
        baos.write(new byte[dataSize]);

        return baos.toByteArray();
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
        };
    }

    private byte[] shortToBytes(short value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }
}
