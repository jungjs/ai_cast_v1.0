package com.aicast.client.ocr;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.*;
import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureVisionClientTest {

    private ImageAnalysisClient imageAnalysisClient;
    private AzureVisionClient azureVisionClient;
    private final double minConfidence = 0.7;

    @BeforeEach
    void setUp() {
        imageAnalysisClient = mock(ImageAnalysisClient.class);
        
        try (MockedConstruction<ImageAnalysisClientBuilder> mockedBuilder = mockConstruction(ImageAnalysisClientBuilder.class, (mock, context) -> {
            when(mock.endpoint(anyString())).thenReturn(mock);
            when(mock.credential(any())).thenReturn(mock);
            when(mock.buildClient()).thenReturn(imageAnalysisClient);
        })) {
            azureVisionClient = new AzureVisionClient("https://mock-endpoint.cognitiveservices.azure.com", "mock-key", minConfidence);
        }
    }

    @Test
    @DisplayName("이미지에서 텍스트 추출 성공 및 신뢰도 필터링 검증")
    void extractText_Success() throws Exception {
        // Given
        MultipartFile mockFile = new MockMultipartFile("image", "test.png", "image/png", new byte[]{1, 2, 3});

        ImageAnalysisResult mockResult = mock(ImageAnalysisResult.class);
        ReadResult mockRead = mock(ReadResult.class);
        DetectedTextBlock mockBlock = mock(DetectedTextBlock.class);
        
        DetectedTextLine mockLinePass = mock(DetectedTextLine.class);
        when(mockLinePass.getText()).thenReturn("신뢰도 통과 텍스트");
        DetectedTextWord mockWordPass = mock(DetectedTextWord.class);
        when(mockWordPass.getConfidence()).thenReturn(0.85);
        when(mockLinePass.getWords()).thenReturn(Collections.singletonList(mockWordPass));

        DetectedTextLine mockLineFail = mock(DetectedTextLine.class);
        DetectedTextWord mockWordFail = mock(DetectedTextWord.class);
        when(mockWordFail.getConfidence()).thenReturn(0.50);
        when(mockLineFail.getWords()).thenReturn(Collections.singletonList(mockWordFail));

        when(imageAnalysisClient.analyze(any(BinaryData.class), any(List.class), any())).thenReturn(mockResult);
        when(mockResult.getRead()).thenReturn(mockRead);
        when(mockRead.getBlocks()).thenReturn(Collections.singletonList(mockBlock));
        when(mockBlock.getLines()).thenReturn(Arrays.asList(mockLinePass, mockLineFail));

        // When
        OcrResult result = azureVisionClient.extractText(mockFile);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("신뢰도 통과 텍스트", result.getExtractedText());
        assertTrue(result.getProcessingTimeMs() >= 0);
    }

    @Test
    @DisplayName("OCR 분석 도중 에러 발생 시 FAILED 처리 검증")
    void extractText_Exception() {
        // Given
        MultipartFile mockFile = new MockMultipartFile("image", "test.png", "image/png", new byte[]{1, 2, 3});
        when(imageAnalysisClient.analyze(any(BinaryData.class), any(List.class), any())).thenThrow(new RuntimeException("OCR service connection failed"));

        // When
        OcrResult result = azureVisionClient.extractText(mockFile);

        // Then
        assertNotNull(result);
        assertTrue(result.getStatus().contains("FAILED"));
        assertNull(result.getExtractedText());
    }
}
