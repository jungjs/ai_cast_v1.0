# Azure Translator 다국어 번역 활용방안

본 문서는 AI Cast 시스템의 다국어 번역 요구사항에 대한 Azure Translator API 활용방안을 정리합니다.

---

## 1. Azure Translator 서비스 개요

Azure Translator는 REST API 기반의 클라우드 번역 서비스로, 135개 이상의 언어를 지원합니다.

### 1.1. AI Cast에서 활용하는 번역 기능

| 기능 | 요구사항 | 설명 |
|:---|:---:|:---|
| **다국어 번역** | F-16 | Azure Translator API를 이용한 요약 내용 다국어 번역 |
| **피벗 번역** | F-17 | 정확성 향상을 위한 KR → EN → Target 피벗 번역 전략 |
| **번역 캐싱** | F-18 | 동일 텍스트에 대한 번역 결과 캐싱 |

### 1.2. 핵심 사양

| 항목 | 내용 |
|:---|:---|
| **API 버전** | v3.0 |
| **엔드포인트** | `https://api.cognitive.microsofttranslator.com` |
| **인증** | `Ocp-Apim-Subscription-Key` + `Ocp-Apim-Subscription-Region` 헤더 |
| **지원 언어** | 135개 이상 |
| **요청 제한** | 요청당 최대 100개 텍스트, 총 10,000자 |
| **다중 언어** | 단일 요청으로 여러 대상 언어 동시 번역 가능 (`to` 파라미터 복수 지정) |

---

## 2. 번역 전략

### 2.1. 직접 번역 vs 피벗 번역 비교

| 항목 | 직접 번역 (KR → Target) | 피벗 번역 (KR → EN → Target) ⭐ |
|:---|:---|:---|
| **API 호출 수** | 1회 | 2회 |
| **번역 정확도** | 언어 쌍에 따라 편차 큼 | 영어를 경유하므로 전반적으로 높음 |
| **지연시간** | 짧음 | 약 2배 |
| **비용** | 낮음 | 약 2배 |
| **적합 시나리오** | 주요 언어 쌍 (KR↔EN, KR↔JA) | 비주류 언어 쌍 (KR→태국어, KR→아랍어 등) |

> **권장**: AI Cast 시스템은 요구사항 F-17에 따라 **피벗 번역 전략**을 기본으로 사용합니다.
> 영어가 가장 풍부한 학습 데이터를 보유하므로, 한국어 → 영어 → 대상 언어 경로가 대부분의 언어 쌍에서 더 정확합니다.

### 2.2. 피벗 번역 흐름

```
NLP 요약 텍스트 (한국어)
        │
        ▼
┌───────────────────┐     ┌───────────────────┐
│ 1단계: KR → EN    │ ──→ │ 2단계: EN → Target│
│ (피벗 번역)       │     │ (최종 번역)       │
└───────────────────┘     └───────────────────┘
        │                         │
        ▼                         ▼
  영어 중간 텍스트          대상 언어 최종 텍스트
```

### 2.3. 번역 캐싱 전략 (F-18)

Azure Translator는 네이티브 캐싱을 제공하지 않으므로, 애플리케이션 레벨에서 구현합니다.

#### 캐시 키 설계

```
캐시 키 = Hash(원본텍스트 + 대상언어코드)
예: SHA-256("오늘 날씨 맑음" + "en") → "a3f2b1..."
```

#### 캐시 구조

| 항목 | 설정 |
|:---|:---|
| **저장소** | Redis (분산 환경) 또는 ConcurrentHashMap (단일 인스턴스) |
| **TTL** | 24시간 (방송 요약 텍스트는 시의성이 있으므로) |
| **캐시 키** | `translate:{sourceText_hash}:{targetLang}` |
| **캐시 값** | 번역된 텍스트 |
| **최대 크기** | 10,000 항목 (LRU 정책) |

---

## 3. API 호출 예시

### 3.1. 단일 언어 번역

```bash
POST https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=ko&to=en
Headers:
  Ocp-Apim-Subscription-Key: <API_KEY>
  Ocp-Apim-Subscription-Region: <REGION>
  Content-Type: application/json

Body:
[{"Text": "오늘 부산 지역에 폭우 주의보가 발령되었습니다."}]
```

**응답:**
```json
[{
  "translations": [{
    "text": "A heavy rain advisory has been issued in the Busan area today.",
    "to": "en"
  }]
}]
```

### 3.2. 다중 언어 동시 번역

```bash
POST https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=ko&to=en&to=ja&to=zh-Hans
```

**응답:**
```json
[{
  "translations": [
    {"text": "A heavy rain advisory...", "to": "en"},
    {"text": "本日、釜山地域に大雨注意報が...", "to": "ja"},
    {"text": "今天釜山地区发布了暴雨预警...", "to": "zh-Hans"}
  ]
}]
```

> **참고**: 다중 언어 동시 번역 시에도 과금은 원본 텍스트 문자 수 × 대상 언어 수로 계산됩니다.

---

## 4. Spring Boot 통합 구현

### 4.1. 설정 파일 (`application.yml`)

```yaml
azure:
  translator:
    key: ${AZURE_TRANSLATOR_KEY}
    region: ${AZURE_TRANSLATOR_REGION}   # 예: koreacentral
    endpoint: https://api.cognitive.microsofttranslator.com
    api-version: "3.0"
    
    # 피벗 번역 설정
    pivot:
      enabled: true
      pivot-language: en    # 피벗 언어 (영어)
    
    # 캐싱 설정
    cache:
      enabled: true
      ttl-hours: 24
      max-size: 10000
```

### 4.2. 서비스 클래스 설계

```java
@Service
@Slf4j
public class TranslationService {
    
    private final WebClient webClient;
    private final TranslationCacheService cacheService;
    private final String apiKey;
    private final String region;
    private final boolean pivotEnabled;
    private final String pivotLanguage;
    
    public TranslationService(
            WebClient.Builder builder,
            TranslationCacheService cacheService,
            @Value("${azure.translator.key}") String apiKey,
            @Value("${azure.translator.region}") String region,
            @Value("${azure.translator.endpoint}") String endpoint,
            @Value("${azure.translator.pivot.enabled}") boolean pivotEnabled,
            @Value("${azure.translator.pivot.pivot-language}") String pivotLanguage) {
        this.webClient = builder.baseUrl(endpoint).build();
        this.cacheService = cacheService;
        this.apiKey = apiKey;
        this.region = region;
        this.pivotEnabled = pivotEnabled;
        this.pivotLanguage = pivotLanguage;
    }
    
    /**
     * 다국어 번역 수행 (F-16)
     * 피벗 번역 전략 적용 (F-17)
     * 캐싱 적용 (F-18)
     */
    public TranslationResult translate(String sourceText, String sourceLang, 
                                        List<String> targetLanguages) {
        long startTime = System.currentTimeMillis();
        Map<String, String> translations = new LinkedHashMap<>();
        
        for (String targetLang : targetLanguages) {
            // 1. 캐시 확인 (F-18)
            String cached = cacheService.get(sourceText, targetLang);
            if (cached != null) {
                translations.put(targetLang, cached);
                log.debug("캐시 히트: {} → {}", sourceLang, targetLang);
                continue;
            }
            
            // 2. 번역 수행
            String translated;
            if (pivotEnabled && !targetLang.equals(pivotLanguage)) {
                // 피벗 번역 (F-17): KR → EN → Target
                translated = pivotTranslate(sourceText, sourceLang, targetLang);
            } else {
                // 직접 번역: KR → Target
                translated = directTranslate(sourceText, sourceLang, targetLang);
            }
            
            // 3. 캐시 저장 (F-18)
            cacheService.put(sourceText, targetLang, translated);
            translations.put(targetLang, translated);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        return new TranslationResult(translations, elapsed, "SUCCESS");
    }
    
    /**
     * 직접 번역
     */
    private String directTranslate(String text, String from, String to) {
        return callTranslatorApi(text, from, to);
    }
    
    /**
     * 피벗 번역 (F-17): source → EN → target
     */
    private String pivotTranslate(String text, String sourceLang, String targetLang) {
        // 1단계: 원본 → 영어
        String englishText = callTranslatorApi(text, sourceLang, pivotLanguage);
        
        // 2단계: 영어 → 대상 언어
        return callTranslatorApi(englishText, pivotLanguage, targetLang);
    }
    
    /**
     * Azure Translator API 호출
     */
    private String callTranslatorApi(String text, String from, String to) {
        String uri = String.format(
            "/translate?api-version=3.0&from=%s&to=%s", from, to);
        
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
        
        // JSON 파싱하여 번역 텍스트 추출
        return parseTranslationResponse(response);
    }
}
```

### 4.3. 캐시 서비스

```java
@Service
public class TranslationCacheService {
    
    private final Cache<String, String> translationCache;
    
    public TranslationCacheService(
            @Value("${azure.translator.cache.max-size}") int maxSize,
            @Value("${azure.translator.cache.ttl-hours}") int ttlHours) {
        this.translationCache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlHours, TimeUnit.HOURS)
            .recordStats()
            .build();
    }
    
    public String get(String sourceText, String targetLang) {
        String key = generateKey(sourceText, targetLang);
        return translationCache.getIfPresent(key);
    }
    
    public void put(String sourceText, String targetLang, String translatedText) {
        String key = generateKey(sourceText, targetLang);
        translationCache.put(key, translatedText);
    }
    
    private String generateKey(String text, String lang) {
        return DigestUtils.sha256Hex(text + ":" + lang);
    }
}
```

### 4.4. 응답 DTO

```java
@Data
@AllArgsConstructor
public class TranslationResult {
    private Map<String, String> translations;  // {언어코드: 번역텍스트}
    private long processingTimeMs;
    private String status;
}
```

### 4.5. Maven 의존성

```xml
<dependencies>
    <!-- Spring WebFlux (WebClient) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Caffeine Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 지원 언어 목록 (AI Cast 주요 대상)

AI Cast 시스템에서 주로 사용할 것으로 예상되는 언어 목록입니다.

| 언어 | 코드 | 비고 |
|:---|:---:|:---|
| 한국어 | `ko` | 원본 언어 (STT/NLP 결과) |
| 영어 | `en` | 피벗 언어, 주요 대상 |
| 일본어 | `ja` | 주요 대상 |
| 중국어 (간체) | `zh-Hans` | 주요 대상 |
| 중국어 (번체) | `zh-Hant` | 대만/홍콩 대상 |
| 베트남어 | `vi` | 다문화 지원 |
| 태국어 | `th` | 다문화 지원 |
| 인도네시아어 | `id` | 다문화 지원 |

> 전체 지원 언어 목록은 `GET https://api.cognitive.microsofttranslator.com/languages?api-version=3.0&scope=translation`으로 조회 가능합니다.

---

## 6. 오류 처리

### 6.1. 주요 오류 유형

| HTTP 코드 | 원인 | 대응 |
|:---:|:---|:---|
| 400 | 잘못된 요청 (텍스트 누락, 잘못된 언어 코드) | 입력 검증 강화 |
| 401 | 인증 실패 (잘못된 API 키) | 키 확인, Key Vault 점검 |
| 403 | 구독 비활성 또는 할당량 초과 | Azure Portal에서 구독 상태 확인 |
| 429 | 요청 제한 초과 (쓰로틀링) | 지수 백오프 재시도 |
| 500 | Azure 서비스 내부 오류 | 재시도 후 지속 시 지원 요청 |

### 6.2. 재시도 전략

```java
@Retryable(
    retryFor = {TranslatorThrottleException.class, WebClientResponseException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2)
)
public String callTranslatorApiWithRetry(String text, String from, String to) {
    return callTranslatorApi(text, from, to);
}
```

---

## 7. 비용 정보

### 7.1. 요금 체계

| 티어 | 요금 | 제한 |
|:---|:---:|:---|
| **무료 (F0)** | 무료 | 월 200만 자 |
| **표준 (S1)** | ~$10 / 100만 자 | 제한 없음 (종량제) |
| **대용량 (S2~S4)** | 할인 적용 | 볼륨 커밋 시 추가 할인 |

> **과금 기준**: 공백·구두점 포함 전체 문자 수 기준으로 과금됩니다.

### 7.2. AI Cast 월간 비용 예측

| 시나리오 | 일일 처리 건수 | 평균 글자/건 | 대상 언어 수 | 피벗 비용 포함 | 월간 예상 비용 |
|:---|:---:|:---:|:---:|:---:|:---:|
| 소규모 | 50건 | 200자 | 3개 | O | ~$9/월 |
| 중규모 | 200건 | 200자 | 5개 | O | ~$60/월 |
| 대규모 | 1,000건 | 200자 | 5개 | O | ~$300/월 |

> **계산식**: 일일 건수 × 글자수 × (대상 언어 수 + 피벗 1회) × 30일 × 단가
> 캐싱(F-18) 적용 시 실제 비용은 20~50% 절감 예상

---

## 8. 피벗 번역 최적화

### 8.1. 스마트 피벗 전략

모든 언어에 피벗을 적용하지 않고, 직접 번역 품질이 충분한 언어 쌍은 직접 번역을 사용합니다.

```java
/**
 * 피벗 번역이 필요한 언어 판단
 * - 주요 언어 쌍(KR↔EN, KR↔JA, KR↔ZH)은 직접 번역
 * - 그 외 언어는 피벗 번역
 */
private static final Set<String> DIRECT_TRANSLATE_LANGS = 
    Set.of("en", "ja", "zh-Hans", "zh-Hant");

private String smartTranslate(String text, String sourceLang, String targetLang) {
    if (DIRECT_TRANSLATE_LANGS.contains(targetLang)) {
        return directTranslate(text, sourceLang, targetLang);
    } else {
        return pivotTranslate(text, sourceLang, targetLang);
    }
}
```

### 8.2. 다중 언어 일괄 요청 최적화

피벗 1단계(KR→EN)는 한 번만 수행하고, 2단계(EN→각 Target)를 일괄 요청으로 처리합니다.

```
입력(한국어) → [1회 호출] KR→EN → [1회 호출] EN→ja,th,vi,id (다중 to 파라미터)
```

총 API 호출: 2회 (피벗 언어 + 비주요 대상 일괄)

---

## 9. AI Cast 파이프라인 내 번역 위치

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│ NLP         │ ──→ │ Translation      │ ──→ │ Image        │
│ (요약 텍스트)│     │ (본 문서)        │     │ Rendering    │
└─────────────┘     │                  │     └──────────────┘
                    │ 1. 캐시 확인     │
                    │ 2. 피벗/직접 번역│
                    │ 3. 캐시 저장     │
                    └──────────────────┘
```

---

## 10. 요구사항 매핑

| 요구사항 ID | 설명 | 관련 섹션 |
|:---:|:---|:---|
| F-16 | Azure Translator API를 이용한 다국어 번역 | §3 API 호출, §4 서비스 클래스 |
| F-17 | 피벗 번역(KR→EN→Target) 전략 | §2.2 피벗 흐름, §8 최적화 |
| F-18 | 동일 텍스트 번역 결과 캐싱 | §2.3 캐싱 전략, §4.3 캐시 서비스 |
| F-11 | 번역 호출 로그 기록 | §4.2 서비스 내 로깅 |

---

**최종 업데이트**: 2026-05-12  
**참조**: [Azure Translator 공식 문서](https://learn.microsoft.com/en-us/azure/ai-services/translator/)
