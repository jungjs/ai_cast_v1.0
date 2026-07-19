package com.aicast.client.nlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AzureOpenAIClient implements NlpClient {

    private final org.springframework.ai.chat.model.ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String FAST_MODE_SYSTEM_PROMPT = """
        당신은 한국어 언어 전문가이자 요약 전문 AI입니다.
        [처리 절차]
        1. 변환: 입력 텍스트의 방언을 표준어로 수정하세요.
        2. 요약: 변환된 텍스트를 200자 이내로 요약하세요.
        [출력 형식]
        반드시 JSON 객체 형식(키 값 refinedText와 summary를 가짐)으로 반환해주세요.
        """;

    public AzureOpenAIClient(org.springframework.ai.chat.model.ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public NlpResult processText(String rawText) {
        long startTime = System.currentTimeMillis();
        
        try {
            org.springframework.ai.chat.messages.SystemMessage systemMessage = new org.springframework.ai.chat.messages.SystemMessage(FAST_MODE_SYSTEM_PROMPT);
            org.springframework.ai.chat.messages.UserMessage userMessage = new org.springframework.ai.chat.messages.UserMessage(rawText);
            org.springframework.ai.chat.prompt.Prompt prompt = new org.springframework.ai.chat.prompt.Prompt(java.util.List.of(systemMessage, userMessage));
            
            org.springframework.ai.chat.model.ChatResponse chatResponse = chatModel.call(prompt);
            String response = chatResponse.getResult().getOutput().getContent();
            
            // JSON 응답 파싱 (마크다운 코드 블록 래핑 제거)
            String sanitized = response.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();
            NlpResult result = objectMapper.readValue(sanitized, NlpResult.class);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setStatus("SUCCESS");
            
            // Usage (토큰 사용량) 추출
            org.springframework.ai.chat.metadata.ChatResponseMetadata metadata = chatResponse.getMetadata();
            if (metadata != null) {
                org.springframework.ai.chat.metadata.Usage usage = metadata.getUsage();
                if (usage != null) {
                    result.setPromptTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : null);
                    result.setCompletionTokens(usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : null);
                    result.setTotalTokens(usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : null);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("NLP 처리 실패: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return NlpResult.builder()
                    .processingTimeMs(elapsed)
                    .status("FAILED: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public String refineText(String rawText) {
        org.springframework.ai.chat.messages.SystemMessage systemMessage = new org.springframework.ai.chat.messages.SystemMessage("사투리를 표준어로 변환하세요. 변환 텍스트만 출력.");
        org.springframework.ai.chat.messages.UserMessage userMessage = new org.springframework.ai.chat.messages.UserMessage(rawText);
        org.springframework.ai.chat.prompt.Prompt prompt = new org.springframework.ai.chat.prompt.Prompt(java.util.List.of(systemMessage, userMessage));
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

    @Override
    public String summarizeText(String refinedText) {
        org.springframework.ai.chat.messages.SystemMessage systemMessage = new org.springframework.ai.chat.messages.SystemMessage("방송용 200자 이내 요약을 작성하세요. 요약만 출력.");
        org.springframework.ai.chat.messages.UserMessage userMessage = new org.springframework.ai.chat.messages.UserMessage(refinedText);
        org.springframework.ai.chat.prompt.Prompt prompt = new org.springframework.ai.chat.prompt.Prompt(java.util.List.of(systemMessage, userMessage));
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
