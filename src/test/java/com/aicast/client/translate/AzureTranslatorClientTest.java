package com.aicast.client.translate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureTranslatorClientTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @Mock private TranslationCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AzureTranslatorClient translatorClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        translatorClient = new AzureTranslatorClient(
                webClientBuilder,
                cacheService,
                objectMapper,
                "test-api-key",
                "test-region",
                "https://api.cognitive.microsofttranslator.com",
                true,
                "en"
        );
    }

    @Test
    @DisplayName("캐시 히트 시 API 호출 없이 캐시된 결과 반환 검증")
    void translate_CacheHit() {
        // Given
        String sourceText = "안녕하세요";
        List<String> targetLangs = Collections.singletonList("en");
        
        when(cacheService.get(sourceText, "en")).thenReturn("Hello (Cached)");

        // When
        TranslationResult result = translatorClient.translate(sourceText, "ko", targetLangs);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("Hello (Cached)", result.getTranslations().get("en"));
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("직접 번역(en) 대상 번역 및 캐시 저장 검증 (Cache Miss)")
    void translate_DirectTranslate() {
        // Given
        String sourceText = "안녕하세요";
        List<String> targetLangs = Collections.singletonList("en");
        String jsonResponse = "[{\"translations\":[{\"text\":\"Hello (API)\",\"to\":\"en\"}]}]";

        when(cacheService.get(sourceText, "en")).thenReturn(null);

        // WebClient 체이닝 모킹
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponse));

        // When
        TranslationResult result = translatorClient.translate(sourceText, "ko", targetLangs);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("Hello (API)", result.getTranslations().get("en"));
        
        verify(cacheService, times(1)).put(sourceText, "en", "Hello (API)");
    }

    @Test
    @DisplayName("비직접 번역(vi) 대상 피벗 번역 및 캐시 저장 검증 (Cache Miss)")
    void translate_PivotTranslate() {
        // Given
        String sourceText = "안녕하세요";
        List<String> targetLangs = Collections.singletonList("vi");
        
        String jsonResponseEn = "[{\"translations\":[{\"text\":\"Hello (Pivot)\",\"to\":\"en\"}]}]";
        String jsonResponseVi = "[{\"translations\":[{\"text\":\"Xin chào (API)\",\"to\":\"vi\"}]}]";

        when(cacheService.get(sourceText, "vi")).thenReturn(null);

        // WebClient 체이닝 모킹
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponseEn), Mono.just(jsonResponseVi));

        // When
        TranslationResult result = translatorClient.translate(sourceText, "ko", targetLangs);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("Xin chào (API)", result.getTranslations().get("vi"));
        
        verify(webClient, times(2)).post();
        verify(cacheService, times(1)).put(sourceText, "vi", "Xin chào (API)");
    }

    @Test
    @DisplayName("9,000자 초과 대용량 텍스트 번역 시 청크 분할 및 연속 번역 수행 검증")
    void translate_LargeTextChunking() {
        // Given
        // 9,500글자의 긴 문장 생성 (9,000자에서 잘라지도록 4,800자 + \n + 4,700자 형태로 배치)
        String firstSentence = "A".repeat(4800) + "\n";
        String secondSentence = "B".repeat(4700);
        String largeSourceText = firstSentence + secondSentence;
        
        List<String> targetLangs = Collections.singletonList("en");
        
        // 각각의 청크에 대한 Mock API 응답 준비
        String jsonResponse1 = "[{\"translations\":[{\"text\":\"TranslatedChunk1\", \"to\":\"en\"}]}]";
        String jsonResponse2 = "[{\"translations\":[{\"text\":\"TranslatedChunk2\", \"to\":\"en\"}]}]";
        
        when(cacheService.get(largeSourceText, "en")).thenReturn(null);
        
        // WebClient 체이닝 모킹
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 순차적으로 2번 호출될 때 각각의 응답 지정
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponse1), Mono.just(jsonResponse2));
        
        // When
        TranslationResult result = translatorClient.translate(largeSourceText, "ko", targetLangs);
        
        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        // 병합된 결과가 리턴되었는지 확인
        assertEquals("TranslatedChunk1TranslatedChunk2", result.getTranslations().get("en"));
        
        // 2번의 API 호출이 일어났는지 확인
        verify(webClient, times(2)).post();
        verify(cacheService, times(1)).put(largeSourceText, "en", "TranslatedChunk1TranslatedChunk2");
    }
}
