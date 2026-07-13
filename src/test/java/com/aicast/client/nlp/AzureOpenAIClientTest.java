package com.aicast.client.nlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureOpenAIClientTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @Mock
    private AssistantMessage assistantMessage;

    private AzureOpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        openAIClient = new AzureOpenAIClient(chatModel, objectMapper);
    }

    @Test
    @DisplayName("텍스트 일괄 정제 및 요약(processText) 성공 검증")
    void processText_Success() {
        // Given
        String rawText = "이거슨 사투리가 섞인 테스트 텍스트여.";
        String jsonResponse = "{\"refinedText\":\"이것은 사투리가 섞인 테스트 텍스트입니다.\",\"summary\":\"테스트 텍스트 요약\"}";

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getContent()).thenReturn(jsonResponse);

        // When
        NlpResult result = openAIClient.processText(rawText);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("이것은 사투리가 섞인 테스트 텍스트입니다.", result.getRefinedText());
        assertEquals("테스트 텍스트 요약", result.getSummary());
        assertTrue(result.getProcessingTimeMs() >= 0);
    }

    @Test
    @DisplayName("processText API 에러 발생 시 FAILED 처리 및 폴백 검증")
    void processText_Exception() {
        // Given
        String rawText = "에러 유발 텍스트";
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("OpenAI quota exceeded"));

        // When
        NlpResult result = openAIClient.processText(rawText);

        // Then
        assertNotNull(result);
        assertTrue(result.getStatus().contains("FAILED"));
        assertNull(result.getRefinedText());
        assertNull(result.getSummary());
    }

    @Test
    @DisplayName("사투리 표준어 정제(refineText) 성공 검증")
    void refineText_Success() {
        // Given
        String rawText = "밥 먹었나?";
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getContent()).thenReturn("밥 먹었니?");

        // When
        String result = openAIClient.refineText(rawText);

        // Then
        assertEquals("밥 먹었니?", result);
    }

    @Test
    @DisplayName("텍스트 요약(summarizeText) 성공 검증")
    void summarizeText_Success() {
        // Given
        String refinedText = "이것은 가상의 표준어 텍스트입니다.";
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getContent()).thenReturn("가상 텍스트 요약.");

        // When
        String result = openAIClient.summarizeText(refinedText);

        // Then
        assertEquals("가상 텍스트 요약.", result);
    }
}
