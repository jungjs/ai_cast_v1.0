package com.aicast.client.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Component
public class AzureTranslatorClient implements TranslateClient {

    private final WebClient webClient;
    private final TranslationCacheService cacheService;
    private final ObjectMapper objectMapper;
    
    private final String apiKey;
    private final String region;
    private final boolean pivotEnabled;
    private final String pivotLanguage;

    private static final Set<String> DIRECT_TRANSLATE_LANGS = Set.of("en", "ja", "zh-Hans", "zh-Hant");

    public AzureTranslatorClient(
            WebClient.Builder builder,
            TranslationCacheService cacheService,
            ObjectMapper objectMapper,
            @Value("${aicast.azure.translator.key:}") String apiKey,
            @Value("${aicast.azure.translator.region:}") String region,
            @Value("${aicast.azure.translator.endpoint:https://api.cognitive.microsofttranslator.com}") String endpoint,
            @Value("${aicast.azure.translator.pivot.enabled:true}") boolean pivotEnabled,
            @Value("${aicast.azure.translator.pivot.pivot-language:en}") String pivotLanguage) {
        this.webClient = builder.baseUrl(endpoint).build();
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.region = region;
        this.pivotEnabled = pivotEnabled;
        this.pivotLanguage = pivotLanguage;
    }

    @Override
    public TranslationResult translate(String sourceText, String sourceLang, List<String> targetLanguages) {
        long startTime = System.currentTimeMillis();
        Map<String, String> translations = new LinkedHashMap<>();

        try {
            for (String targetLang : targetLanguages) {
                // 1. 캐시 확인
                String cached = cacheService.get(sourceText, targetLang);
                if (cached != null) {
                    translations.put(targetLang, cached);
                    log.debug("번역 캐시 히트: {} -> {}", sourceLang, targetLang);
                    continue;
                }

                // 2. 번역 수행
                String translated;
                if (pivotEnabled && !DIRECT_TRANSLATE_LANGS.contains(targetLang)) {
                    translated = pivotTranslate(sourceText, sourceLang, targetLang);
                } else {
                    translated = directTranslate(sourceText, sourceLang, targetLang);
                }

                // 3. 캐시 저장
                cacheService.put(sourceText, targetLang, translated);
                translations.put(targetLang, translated);
            }
            int promptT = sourceText != null ? sourceText.length() : 0;
            int completionT = translations.values().stream()
                    .filter(Objects::nonNull)
                    .mapToInt(String::length)
                    .sum();
            int totalT = promptT + completionT;

            return TranslationResult.builder()
                    .translations(translations)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .status("SUCCESS")
                    .promptTokens(promptT)
                    .completionTokens(completionT)
                    .totalTokens(totalT)
                    .build();
        } catch (Exception e) {
            log.error("번역 API 호출 실패: {}", e.getMessage(), e);
            return TranslationResult.builder()
                    .translations(translations)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .status("FAILED: " + e.getMessage())
                    .build();
        }
    }

    private String directTranslate(String text, String from, String to) throws Exception {
        return callTranslatorApi(text, from, to);
    }

    private String pivotTranslate(String text, String sourceLang, String targetLang) throws Exception {
        // 1단계: 원본 -> 피벗어(EN)
        String englishText = callTranslatorApi(text, sourceLang, pivotLanguage);
        // 2단계: 피벗어(EN) -> 타겟어
        return callTranslatorApi(englishText, pivotLanguage, targetLang);
    }

    private String callTranslatorApi(String text, String from, String to) throws Exception {
        if (text == null) {
            return "";
        }

        int limit = 9000;
        if (text.length() <= limit) {
            return callTranslatorApiRaw(text, from, to);
        }

        log.info("번역 텍스트가 {}자를 초과하여 스마트 분할 번역을 수행합니다. (총 길이: {})", limit, text.length());
        List<String> chunks = splitIntoChunks(text, limit);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.info("분할 번역 수행 중: {} / {}", i + 1, chunks.size());
            String translatedChunk = callTranslatorApiRaw(chunk, from, to);
            sb.append(translatedChunk);
        }

        return sb.toString();
    }

    private String callTranslatorApiRaw(String text, String from, String to) throws Exception {
        String uri = String.format("/translate?api-version=3.0&from=%s&to=%s", from, to);
        List<Map<String, String>> body = List.of(Map.of("Text", text));

        String response = webClient.post()
                .uri(uri)
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .header("Ocp-Apim-Subscription-Region", region)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseTranslationResponse(response);
    }

    private List<String> splitIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = start + maxChunkSize;
            if (end >= length) {
                chunks.add(text.substring(start));
                break;
            }

            // 문장 종결 기호(줄바꿈, 마침표, 물음표, 느낌표)를 뒤에서부터 역탐색하여 잘라냄
            int limit = start + (int)(maxChunkSize * 0.7); // 최소 70% 구역까지 탐색
            int cutIndex = -1;

            for (int i = end; i >= limit; i--) {
                char c = text.charAt(i);
                if (c == '\n' || c == '.' || c == '?' || c == '!') {
                    cutIndex = i + 1; // 종결자 바로 뒤에서 끊음
                    break;
                }
            }

            if (cutIndex == -1) {
                // 마땅한 종결자가 없으면 공백(' ')을 기준으로 탐색
                for (int i = end; i >= limit; i--) {
                    char c = text.charAt(i);
                    if (c == ' ') {
                        cutIndex = i + 1;
                        break;
                    }
                }
            }

            if (cutIndex == -1) {
                // 이마저도 없으면 어쩔 수 없이 절대 크기로 분할
                cutIndex = end;
            }

            chunks.add(text.substring(start, cutIndex));
            start = cutIndex;
        }

        return chunks;
    }

    private String parseTranslationResponse(String jsonResponse) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        return rootNode.get(0).get("translations").get(0).get("text").asText();
    }
}
