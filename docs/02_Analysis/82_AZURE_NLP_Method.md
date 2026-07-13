# Azure OpenAI NLP 활용방안

본 문서는 AI Cast 시스템의 자연어 처리(NLP) 요구사항(F-22, F-14, F-15)에 대한 Azure OpenAI 서비스 활용방안을 정리합니다.

---

## 1. Azure OpenAI 서비스 개요

Azure OpenAI Service는 GPT-4o 등 OpenAI 모델을 Azure 클라우드에서 엔터프라이즈 수준의 보안과 규정 준수와 함께 제공합니다.

### 1.1. AI Cast에서 활용하는 NLP 기능

| 기능 | 요구사항 | 설명 |
|:---|:---:|:---|
| **사투리 → 표준어 변환** | F-22 | 지역 방송 음성에서 변환된 텍스트의 방언을 표준 한국어로 정제 |
| **방송용 요약 생성** | F-14 | 표준화된 텍스트를 간결한 방송용 요약으로 변환 |
| **FAST_MODE (정제+요약 통합)** | F-15 | 정제와 요약을 단일 프로세스로 수행하여 지연시간 최소화 |

### 1.2. 사용 가능 모델 비교

| 모델 | 특징 | 권장 용도 | 비용 (1M 토큰) |
|:---|:---|:---|:---:|
| **GPT-4o** | 최고 성능, 한국어 이해도 우수 | 고품질 정제+요약 | 입력 ~$2.50 / 출력 ~$10.00 |
| **GPT-4o mini** | 경량, 빠른 응답, 비용 효율적 | 단순 정제, 대량 처리 | GPT-4o 대비 대폭 저렴 |
| **GPT-3.5 Turbo** | 레거시, 가장 저렴 | 단순 텍스트 처리 | 최저 비용 |

> **권장**: AI Cast 시스템은 사투리 변환의 정확도가 중요하므로 **GPT-4o**를 기본 모델로 사용합니다.

---

## 2. 접근 방식 비교

### 2.1. Spring AI vs Azure OpenAI SDK

| 항목 | Spring AI (권장) | Azure OpenAI Java SDK |
|:---|:---|:---|
| **추상화 수준** | 높음 (Spring 생태계 통합) | 낮음 (세밀한 제어) |
| **의존성** | `spring-ai-azure-openai-spring-boot-starter` | `azure-ai-openai` |
| **모델 교체** | 설정 변경만으로 가능 | 코드 수정 필요 |
| **프롬프트 관리** | 템플릿 기반 | 수동 구성 |
| **Spring Boot 통합** | 네이티브 | 수동 Bean 구성 필요 |
| **AI Cast 적합도** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 3. 프롬프트 엔지니어링

### 3.1. 사투리 → 표준어 변환 프롬프트 (F-22)

```text
[System Prompt]
당신은 한국어 언어 전문가입니다. 사용자가 입력한 텍스트에 포함된 지역 방언(사투리)을
문맥을 해치지 않으면서 자연스러운 표준어로 변환하세요.

[규칙]
1. 방언, 비속어, 문법적 오류를 표준 한국어 문법과 어휘에 맞게 수정하세요.
2. 원문의 의미와 정보를 변경하지 마세요.
3. 고유명사(지명, 인명)는 변경하지 마세요.
4. 변환된 텍스트만 출력하세요. 설명을 추가하지 마세요.
```

### 3.2. 방송용 요약 프롬프트 (F-14)

```text
[System Prompt]
당신은 한국 방송 뉴스를 전문적으로 요약하는 AI 에디터입니다.
객관적이고 정확하며, 시청자가 핵심 내용을 빠르게 파악할 수 있도록 간결하게 요약하세요.

[규칙]
1. 항상 한국어로 답변하세요.
2. 전문 용어는 문맥에 맞게 쉽게 풀어쓰세요.
3. 편향되지 않은 중립적인 어조를 유지하세요.
4. 중요한 수치나 날짜는 반드시 포함하세요.
5. 200자 이내로 요약하세요.
6. 요약 텍스트만 출력하세요.
```

### 3.3. FAST_MODE 통합 프롬프트 (F-15) ⭐ 권장

정제와 요약을 단일 API 호출로 수행하여 지연시간과 비용을 절감합니다.

```text
[System Prompt]
당신은 한국어 언어 전문가이자 요약 전문 AI입니다.

[처리 절차]
1. 변환: 입력 텍스트의 방언, 비속어, 문법적 오류를 표준어로 수정하세요.
2. 요약: 변환된 표준어 텍스트를 바탕으로 핵심 내용을 간결하게 요약하세요.

[출력 형식 - 반드시 아래 JSON 형식으로 출력]
{
  "refined_text": "(표준어로 변환된 전체 텍스트)",
  "summary": "(200자 이내 요약)"
}

[규칙]
- 원문의 의미와 정보를 변경하지 마세요.
- 고유명사(지명, 인명)는 변경하지 마세요.
- 중요한 수치나 날짜는 반드시 포함하세요.
- JSON 형식 외의 텍스트를 출력하지 마세요.
```

### 3.4. 파라미터 권장 설정

| 파라미터 | 권장값 | 설명 |
|:---|:---:|:---|
| **temperature** | 0.2 ~ 0.3 | 사실 기반 작업이므로 낮게 설정하여 환각(Hallucination) 방지 |
| **max_tokens** | 1000 ~ 2000 | 요약 출력 길이에 따라 조정, 한국어는 토큰 소모량이 큼 |
| **top_p** | 0.9 | temperature와 함께 사용하여 출력 다양성 제어 |
| **frequency_penalty** | 0.0 | 반복 억제 불필요 (요약은 원문 기반) |

---

## 4. Spring Boot 통합 구현

### 4.1. Maven 의존성

**방식 A: Spring AI (권장)**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-azure-openai-spring-boot-starter</artifactId>
</dependency>
```

**방식 B: Azure OpenAI SDK (직접 제어)**
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-ai-openai</artifactId>
    <version>1.0.0-beta.16</version>
</dependency>
```

### 4.2. 설정 파일 (`application.yml`)

```yaml
# Spring AI 방식
spring:
  ai:
    azure:
      openai:
        api-key: ${AZURE_OPENAI_API_KEY}
        endpoint: ${AZURE_OPENAI_ENDPOINT}
        chat:
          options:
            deployment-name: gpt-4o
            temperature: 0.2
            max-tokens: 1500

# 또는 직접 SDK 방식
azure:
  openai:
    key: ${AZURE_OPENAI_API_KEY}
    endpoint: ${AZURE_OPENAI_ENDPOINT}
    deployment-name: gpt-4o
```

### 4.3. 서비스 클래스 설계

#### 방식 A: Spring AI 활용 (권장)

```java
@Service
@Slf4j
public class NlpService {
    
    private final ChatClient chatClient;
    
    // 프롬프트 템플릿 (외부 파일 관리 가능)
    private static final String FAST_MODE_SYSTEM_PROMPT = """
        당신은 한국어 언어 전문가이자 요약 전문 AI입니다.
        [처리 절차]
        1. 변환: 입력 텍스트의 방언을 표준어로 수정하세요.
        2. 요약: 변환된 텍스트를 200자 이내로 요약하세요.
        [출력 형식 - JSON]
        {"refined_text": "...", "summary": "..."}
        """;
    
    public NlpService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem(FAST_MODE_SYSTEM_PROMPT)
            .build();
    }
    
    /**
     * FAST_MODE: 정제 + 요약 통합 처리 (F-15)
     */
    public NlpResult processText(String rawText) {
        long startTime = System.currentTimeMillis();
        
        String response = chatClient.prompt()
            .user(rawText)
            .call()
            .content();
        
        // JSON 파싱
        NlpResult result = parseJsonResponse(response);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        log.info("NLP 처리 완료 - 소요시간: {}ms", result.getProcessingTimeMs());
        return result;
    }
    
    /**
     * 사투리 표준어 변환만 수행 (F-22)
     */
    public String refineText(String rawText) {
        return chatClient.prompt()
            .system("사투리를 표준어로 변환하세요. 변환 텍스트만 출력.")
            .user(rawText)
            .call()
            .content();
    }
    
    /**
     * 요약만 수행 (F-14)
     */
    public String summarizeText(String refinedText) {
        return chatClient.prompt()
            .system("방송용 200자 이내 요약을 작성하세요. 요약만 출력.")
            .user(refinedText)
            .call()
            .content();
    }
}
```

#### 방식 B: Azure OpenAI SDK 활용

```java
@Service
@Slf4j
public class NlpService {
    
    private final OpenAIClient openAIClient;
    private final String deploymentName;
    
    public NlpService(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.key}") String key,
            @Value("${azure.openai.deployment-name}") String deploymentName) {
        this.deploymentName = deploymentName;
        this.openAIClient = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(key))
            .buildClient();
    }
    
    public NlpResult processText(String rawText) {
        List<ChatRequestMessage> messages = List.of(
            new ChatRequestSystemMessage(FAST_MODE_SYSTEM_PROMPT),
            new ChatRequestUserMessage(rawText)
        );
        
        ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
            .setTemperature(0.2)
            .setMaxTokens(1500);
        
        ChatCompletions completions = openAIClient
            .getChatCompletions(deploymentName, options);
        
        String content = completions.getChoices().get(0)
            .getMessage().getContent();
        
        return parseJsonResponse(content);
    }
}
```

### 4.4. 응답 DTO

```java
@Data
public class NlpResult {
    private String refinedText;    // 표준어 변환 텍스트
    private String summary;        // 요약 텍스트
    private long processingTimeMs; // 처리 소요시간
    private String status;         // SUCCESS / FAILED
}
```

---

## 5. 처리 모드 비교 (일반 vs FAST_MODE)

### 5.1. 일반 모드 (2단계 처리)

```
입력 텍스트 → [API 호출 1] 표준어 변환(F-22) → [API 호출 2] 요약 생성(F-14) → 결과
```

- API 호출 수: **2회**
- 장점: 각 단계별 결과 확인 및 중간 개입 가능
- 단점: 지연시간 증가, 비용 2배

### 5.2. FAST_MODE (단일 처리) ⭐ 권장

```
입력 텍스트 → [API 호출 1] 정제+요약 통합(F-15) → 결과(JSON)
```

- API 호출 수: **1회**
- 장점: 지연시간 50% 감소, 비용 절감
- 단점: 중간 결과 확인 불가

### 5.3. 성능 비교 예상치

| 항목 | 일반 모드 | FAST_MODE |
|:---|:---:|:---:|
| API 호출 수 | 2회 | 1회 |
| 예상 지연시간 | 3~6초 | 2~4초 |
| 토큰 사용량 (상대적) | 1.5x ~ 2x | 1x |
| 비용 (상대적) | 높음 | 낮음 |

---

## 6. 오류 처리 및 안정성

### 6.1. 주요 오류 유형

| 오류 | 원인 | 대응 |
|:---|:---|:---|
| HTTP 429 | 요청 제한 초과 (쓰로틀링) | 지수 백오프 재시도 |
| HTTP 401/403 | 인증 실패 | API 키/엔드포인트 확인 |
| 컨텍스트 길이 초과 | 입력 텍스트가 모델 한계 초과 | 텍스트 분할 처리 |
| JSON 파싱 실패 | 모델 출력이 JSON 형식 불일치 | 재시도 또는 폴백 처리 |
| 환각(Hallucination) | 모델이 사실과 다른 정보 생성 | 낮은 temperature, 프롬프트 강화 |

### 6.2. 재시도 전략

```java
@Retryable(
    retryFor = {AzureOpenAIThrottleException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public NlpResult processWithRetry(String text) {
    return processText(text);
}
```

### 6.3. 긴 텍스트 분할 처리

```java
/**
 * 컨텍스트 윈도우 초과 시 텍스트를 분할하여 처리
 * GPT-4o 기준 최대 128K 토큰 (입력+출력 합계)
 */
public NlpResult processLongText(String longText) {
    if (estimateTokenCount(longText) > MAX_INPUT_TOKENS) {
        List<String> chunks = splitByParagraph(longText);
        // 각 청크별 처리 후 결과 병합
        return mergeResults(chunks.stream()
            .map(this::processText)
            .collect(Collectors.toList()));
    }
    return processText(longText);
}
```

---

## 7. 비용 관리

### 7.1. 비용 최적화 전략

| 전략 | 설명 |
|:---|:---|
| **FAST_MODE 사용** | 2회 호출을 1회로 줄여 비용 50% 절감 |
| **프롬프트 캐싱** | 시스템 프롬프트를 앞에 배치하여 Azure 캐시 적중률 극대화 |
| **max_tokens 제한** | 필요한 최소 출력 길이로 설정 |
| **모델 계층화** | 단순 작업은 GPT-4o mini, 복잡한 작업은 GPT-4o 사용 |
| **Batch API** | 시간에 민감하지 않은 대량 처리 시 50% 할인 |

### 7.2. AI Cast 월간 비용 예측

| 시나리오 | 일일 처리 건수 | 평균 토큰/건 | 월간 예상 비용 |
|:---|:---:|:---:|:---:|
| 소규모 | 50건 | ~2,000 | ~$10/월 |
| 중규모 | 200건 | ~2,000 | ~$40/월 |
| 대규모 | 1,000건 | ~2,000 | ~$200/월 |

> 기준: GPT-4o, FAST_MODE, 입력 ~1,500 + 출력 ~500 토큰/건

---

## 8. AI Cast 파이프라인 내 NLP 위치

```
음성/텍스트/이미지 입력
        │
        ▼
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│ STT / OCR    │ ──→ │  NLP (본 문서)   │ ──→ │ Translation  │ ──→ Image Rendering
│ (원본 텍스트) │     │ 정제+요약(F-15) │     │ 다국어 번역   │
└──────────────┘     └─────────────────┘     └──────────────┘
```

---

## 9. 요구사항 매핑

| 요구사항 ID | 설명 | 관련 섹션 |
|:---:|:---|:---|
| F-22 | Azure OpenAI를 통한 사투리 표준어 변환 | §3.1 프롬프트, §4.3 서비스 클래스 |
| F-14 | 표준화된 텍스트의 방송용 요약 생성 | §3.2 프롬프트, §4.3 서비스 클래스 |
| F-15 | FAST_MODE (정제+요약 단일 프로세스) | §3.3 통합 프롬프트, §5 모드 비교 |
| F-11 | NLP 호출 로그 기록 | §4.3 서비스 내 로깅 |

---

**최종 업데이트**: 2026-05-12
**참조**: [Azure OpenAI Service 공식 문서](https://learn.microsoft.com/en-us/azure/ai-services/openai/)
