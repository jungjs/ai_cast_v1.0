package com.aicast.common.aop;

import com.aicast.client.nlp.NlpResult;
import com.aicast.client.translate.TranslationResult;
import com.aicast.domain.log.TbAiSvcLog;
import com.aicast.service.log.AiSvcLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSvcLogAspectTest {

    @Mock
    private AiSvcLogService aiSvcLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private AiSvcLogAspect aiSvcLogAspect;

    static class FakeSpeechClient {}
    static class FakeOpenAIClient {}
    static class FakeTranslatorClient {}
    static class FakeVisionClient {}
    static class FakeBlobClient {}
    static class FakeUnknownClient {}

    @BeforeEach
    void setUp() {
        MDC.put("correlationId", "corr-aop-123");
        MDC.put("apiKey", "apikey-aop-456");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("AI 서비스 호출 성공 시 실행시간 측정 및 로그 저장 검증")
    void logAiServiceCall_Success() throws Throwable {
        // Given
        FakeSpeechClient target = new FakeSpeechClient();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("recognize");
        when(joinPoint.proceed()).thenReturn("음성인식완료");

        // When
        Object result = aiSvcLogAspect.logAiServiceCall(joinPoint);

        // Then
        assertEquals("음성인식완료", result);

        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertEquals("corr-aop-123", savedLog.getCorrId());
        assertEquals("apikey-aop-456", savedLog.getApiKey());
        assertEquals("STT", savedLog.getSvcType());
        assertTrue(savedLog.getIsOk());
        assertNull(savedLog.getErrMsg());
        assertTrue(savedLog.getProcMs() >= 0);
    }

    @Test
    @DisplayName("AI 서비스 호출 실패 시 예외 던짐 및 에러 로그 기록 검증")
    void logAiServiceCall_Exception() throws Throwable {
        // Given
        FakeOpenAIClient target = new FakeOpenAIClient();
        RuntimeException exception = new RuntimeException("API Timeout Exception");
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("summarize");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            aiSvcLogAspect.logAiServiceCall(joinPoint);
        });
        assertEquals("API Timeout Exception", thrown.getMessage());

        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertEquals("NLP", savedLog.getSvcType());
        assertFalse(savedLog.getIsOk());
        assertEquals("API Timeout Exception", savedLog.getErrMsg());
    }

    @Test
    @DisplayName("MDC에 정보가 없을 때 UNKNOWN으로 기본값 바인딩 검증")
    void logAiServiceCall_MdcMissing() throws Throwable {
        // Given
        MDC.clear();
        FakeTranslatorClient target = new FakeTranslatorClient();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("translate");
        when(joinPoint.proceed()).thenReturn("translated text");

        // When
        aiSvcLogAspect.logAiServiceCall(joinPoint);

        // Then
        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertEquals("UNKNOWN", savedLog.getCorrId());
        assertEquals("UNKNOWN", savedLog.getApiKey());
        assertEquals("TRANSLATE", savedLog.getSvcType());
    }

    @Test
    @DisplayName("클래스명 접미사에 따른 svcType 도출(deriveSvcType) 분기 검증")
    void logAiServiceCall_DeriveSvcType() throws Throwable {
        // Test Vision -> OCR
        reset(aiSvcLogService);
        FakeVisionClient visionTarget = new FakeVisionClient();
        when(joinPoint.getTarget()).thenReturn(visionTarget);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("ocr");
        aiSvcLogAspect.logAiServiceCall(joinPoint);
        
        ArgumentCaptor<TbAiSvcLog> visionCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService).saveLog(visionCaptor.capture());
        assertEquals("OCR", visionCaptor.getValue().getSvcType());

        // Test Blob -> STORAGE
        reset(aiSvcLogService);
        FakeBlobClient blobTarget = new FakeBlobClient();
        when(joinPoint.getTarget()).thenReturn(blobTarget);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("upload");
        aiSvcLogAspect.logAiServiceCall(joinPoint);
        
        ArgumentCaptor<TbAiSvcLog> blobCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService).saveLog(blobCaptor.capture());
        assertEquals("STORAGE", blobCaptor.getValue().getSvcType());

        // Test Unknown -> UNKNOWN
        reset(aiSvcLogService);
        FakeUnknownClient unknownTarget = new FakeUnknownClient();
        when(joinPoint.getTarget()).thenReturn(unknownTarget);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("doSomething");
        aiSvcLogAspect.logAiServiceCall(joinPoint);
        
        ArgumentCaptor<TbAiSvcLog> unknownCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService).saveLog(unknownCaptor.capture());
        assertEquals("UNKNOWN", unknownCaptor.getValue().getSvcType());
    }

    @Test
    @DisplayName("NLP 호출 결과에서 토큰 사용량 추출 검증")
    void logAiServiceCall_NlpResult_TokensExtracted() throws Throwable {
        // Given
        FakeOpenAIClient target = new FakeOpenAIClient();
        NlpResult nlpResult = NlpResult.builder()
                .refinedText("변환된 텍스트")
                .summary("요약")
                .processingTimeMs(150)
                .status("SUCCESS")
                .promptTokens(120)
                .completionTokens(80)
                .totalTokens(200)
                .build();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("processText");
        when(joinPoint.proceed()).thenReturn(nlpResult);

        // When
        aiSvcLogAspect.logAiServiceCall(joinPoint);

        // Then
        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertEquals("NLP", savedLog.getSvcType());
        assertEquals(120, savedLog.getPromptTokens());
        assertEquals(80, savedLog.getCompletionTokens());
        assertEquals(200, savedLog.getTotalTokens());
    }

    @Test
    @DisplayName("Translation 호출 결과에서 토큰 사용량 및 resSize 추출 검증")
    void logAiServiceCall_TranslationResult_TokensAndSizeExtracted() throws Throwable {
        // Given
        FakeTranslatorClient target = new FakeTranslatorClient();
        TranslationResult trResult = TranslationResult.builder()
                .translations(Map.of("ko", "번역된텍스트", "en", "TranslatedText"))
                .processingTimeMs(200)
                .status("SUCCESS")
                .promptTokens(50)
                .completionTokens(30)
                .totalTokens(80)
                .build();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("translate");
        when(joinPoint.proceed()).thenReturn(trResult);

        // When
        aiSvcLogAspect.logAiServiceCall(joinPoint);

        // Then
        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertEquals("TRANSLATE", savedLog.getSvcType());
        assertEquals(50, savedLog.getPromptTokens());
        assertEquals(30, savedLog.getCompletionTokens());
        assertEquals(80, savedLog.getTotalTokens());
        assertNotNull(savedLog.getResSize());
        assertTrue(savedLog.getResSize() > 0);
    }

    @Test
    @DisplayName("토큰이 없는 일반 결과는 토큰 필드가 null로 기록 검증")
    void logAiServiceCall_GenericResult_TokensNull() throws Throwable {
        // Given
        FakeSpeechClient target = new FakeSpeechClient();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("recognize");
        when(joinPoint.proceed()).thenReturn("일반 결과 문자열");

        // When
        aiSvcLogAspect.logAiServiceCall(joinPoint);

        // Then
        ArgumentCaptor<TbAiSvcLog> logCaptor = ArgumentCaptor.forClass(TbAiSvcLog.class);
        verify(aiSvcLogService, times(1)).saveLog(logCaptor.capture());

        TbAiSvcLog savedLog = logCaptor.getValue();
        assertNull(savedLog.getPromptTokens());
        assertNull(savedLog.getCompletionTokens());
        assertNull(savedLog.getTotalTokens());
        assertNotNull(savedLog.getResSize());
    }
}
