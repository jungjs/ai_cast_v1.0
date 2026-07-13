package com.aicast.service;

import com.aicast.client.nlp.NlpClient;
import com.aicast.client.nlp.NlpResult;
import com.aicast.client.ocr.OcrClient;
import com.aicast.client.ocr.OcrResult;
import com.aicast.client.storage.BlobClient;
import com.aicast.client.stt.SttClient;
import com.aicast.client.translate.TranslateClient;
import com.aicast.client.translate.TranslationResult;
import com.aicast.dto.response.PipelineResponse;
import com.aicast.engine.ImageRenderingEngine;
import com.aicast.service.pipeline.DefaultPipelineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aicast.client.ocr.OcrResult;

@ExtendWith(MockitoExtension.class)
class DefaultPipelineServiceTest {

    @Mock private SttClient sttClient;
    @Mock private NlpClient nlpClient;
    @Mock private TranslateClient translateClient;
    @Mock private OcrClient ocrClient;
    @Mock private BlobClient blobClient;
    @Mock private ImageRenderingEngine renderingEngine;

    @InjectMocks
    private DefaultPipelineService pipelineService;

    @Test
    @DisplayName("Audio 파이프라인 정상 수행 검증")
    void executeAudio_Success() throws Exception {
        // Given
        MultipartFile mockAudio = mock(MultipartFile.class);
        List<String> targetLangs = Collections.singletonList("EN");
        
        when(sttClient.speechToText(mockAudio)).thenReturn("음성 인식 결과");
        
        NlpResult mockNlp = mock(NlpResult.class);
        when(mockNlp.getStatus()).thenReturn("SUCCESS");
        when(mockNlp.getRefinedText()).thenReturn("정제된 텍스트");
        when(mockNlp.getSummary()).thenReturn("요약된 텍스트");
        when(mockNlp.getProcessingTimeMs()).thenReturn(100L);
        when(nlpClient.processText("음성 인식 결과")).thenReturn(mockNlp);
        
        TranslationResult mockTrans = mock(TranslationResult.class);
        when(mockTrans.getStatus()).thenReturn("SUCCESS");
        Map<String, String> transMap = new HashMap<>();
        transMap.put("EN", "Translated Text");
        when(mockTrans.getTranslations()).thenReturn(transMap);
        when(translateClient.translate("요약된 텍스트", "ko", targetLangs)).thenReturn(mockTrans);
        
        when(renderingEngine.render(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(blobClient.upload(any(), anyString())).thenReturn("http://blob.url/image.png");

        // When
        PipelineResponse response = pipelineService.executeAudio(mockAudio, targetLangs);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("http://blob.url/image.png", response.getImageUrl());
        assertEquals("Translated Text", response.getTranslations().get("EN"));
        
        verify(sttClient, times(1)).speechToText(mockAudio);
        verify(nlpClient, times(1)).processText(anyString());
        verify(translateClient, times(1)).translate(anyString(), anyString(), any());
        verify(renderingEngine, times(1)).render(any(), any());
        verify(blobClient, times(1)).upload(any(), anyString());
    }

    @Test
    @DisplayName("Text 파이프라인 정상 수행 검증 (STT 생략)")
    void executeText_Success() throws Exception {
        // Given
        String textInput = "재난 문자 텍스트입니다.";
        List<String> targetLangs = Collections.singletonList("EN");
        
        NlpResult mockNlp = mock(NlpResult.class);
        when(mockNlp.getStatus()).thenReturn("SUCCESS");
        when(mockNlp.getRefinedText()).thenReturn("정제된 텍스트");
        when(mockNlp.getSummary()).thenReturn("요약된 텍스트");
        when(mockNlp.getProcessingTimeMs()).thenReturn(80L);
        when(nlpClient.processText(textInput)).thenReturn(mockNlp);
        
        TranslationResult mockTrans = mock(TranslationResult.class);
        when(mockTrans.getStatus()).thenReturn("SUCCESS");
        Map<String, String> transMap = new HashMap<>();
        transMap.put("EN", "Translated Text");
        when(mockTrans.getTranslations()).thenReturn(transMap);
        when(translateClient.translate("요약된 텍스트", "ko", targetLangs)).thenReturn(mockTrans);
        
        when(renderingEngine.render(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(blobClient.upload(any(), anyString())).thenReturn("http://blob.url/text_image.png");

        // When
        PipelineResponse response = pipelineService.executeText(textInput, targetLangs);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("http://blob.url/text_image.png", response.getImageUrl());
        
        verify(sttClient, never()).speechToText(any());
        verify(nlpClient, times(1)).processText(anyString());
    }

    @Test
    @DisplayName("Image 파이프라인 정상 수행 검증 (OCR -> NLP -> 번역 -> 렌더링 -> 업로드)")
    void executeImage_Success() throws Exception {
        // Given
        MultipartFile mockImage = mock(MultipartFile.class);
        List<String> targetLangs = List.of("EN", "JA");

        OcrResult mockOcr = mock(OcrResult.class);
        when(mockOcr.getStatus()).thenReturn("SUCCESS");
        when(mockOcr.getExtractedText()).thenReturn("OCR로 추출된 텍스트");
        when(ocrClient.extractText(mockImage)).thenReturn(mockOcr);

        NlpResult mockNlp = mock(NlpResult.class);
        when(mockNlp.getStatus()).thenReturn("SUCCESS");
        when(mockNlp.getRefinedText()).thenReturn("정제된 텍스트");
        when(mockNlp.getSummary()).thenReturn("요약된 텍스트");
        when(mockNlp.getProcessingTimeMs()).thenReturn(90L);
        when(nlpClient.processText("OCR로 추출된 텍스트")).thenReturn(mockNlp);

        TranslationResult mockTrans = mock(TranslationResult.class);
        when(mockTrans.getStatus()).thenReturn("SUCCESS");
        Map<String, String> transMap = new HashMap<>();
        transMap.put("EN", "English Text");
        transMap.put("JA", "日本語テキスト");
        when(mockTrans.getTranslations()).thenReturn(transMap);
        when(translateClient.translate("요약된 텍스트", "ko", targetLangs)).thenReturn(mockTrans);

        when(renderingEngine.render(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(blobClient.upload(any(), anyString())).thenReturn("http://blob.url/image.png");

        // When
        PipelineResponse response = pipelineService.executeImage(mockImage, targetLangs);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("http://blob.url/image.png", response.getImageUrl());
        assertEquals("OCR로 추출된 텍스트", response.getOriginalText());
        assertEquals("English Text", response.getTranslations().get("EN"));

        verify(ocrClient, times(1)).extractText(mockImage);
        verify(nlpClient, times(1)).processText(anyString());
        verify(translateClient, times(1)).translate(anyString(), anyString(), any());
        verify(renderingEngine, times(1)).render(any(), any());
        verify(blobClient, times(1)).upload(any(), anyString());
    }

    @Test
    @DisplayName("하위 AI 클라이언트 오류 시 Fallback 처리 검증")
    void executePipeline_ClientError() throws Exception {
        // Given
        MultipartFile mockAudio = mock(MultipartFile.class);
        List<String> targetLangs = Collections.singletonList("EN");

        when(sttClient.speechToText(mockAudio)).thenThrow(new RuntimeException("STT Service Unavailable"));

        // When
        PipelineResponse response = pipelineService.executeAudio(mockAudio, targetLangs);

        // Then
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("STT Service Unavailable"));

        verify(sttClient, times(1)).speechToText(mockAudio);
        verifyNoInteractions(nlpClient, translateClient, renderingEngine, blobClient);
    }
}
