# AI Cast 시스템 로깅 전략

본 문서는 AI Cast 시스템의 비기능 요구사항 중 로깅 관련 항목(NF-01, NF-03, NF-05)의 구현 전략을 정의합니다.

---

## 1. 대상 요구사항

| 요구사항 ID | 설명 |
|:---:|:---|
| **NF-01** | 단계별 처리 시간 기록 및 비동기 처리 적용 |
| **NF-03** | Correlation ID 기반의 전 과정 추적 |
| **NF-05** | 장애 발생 시 관리자 알림 및 정밀 에러 로그 기록 |

---

## 2. 기술 스택

| 항목 | 선택 | 비고 |
|:---|:---|:---|
| **로깅 Facade** | SLF4J | Spring Boot 기본 내장 |
| **로깅 구현체** | Logback | Spring Boot 기본 내장 |
| **로그 포맷** | JSON 구조화 | `logstash-logback-encoder` 활용 |
| **비동기 로깅** | AsyncAppender | 고부하 시 메인 스레드 블로킹 방지 |
| **추적** | MDC (Mapped Diagnostic Context) | Correlation ID 전파 |
| **알림** | Slack Webhook | 장애 알림 채널 |

---

## 3. NF-01: 단계별 처리 시간 기록

### 3.1. 개요

AI Cast 파이프라인의 각 단계(STT → NLP → TRANSLATE → IMAGE_GENERATION 등)별 처리 시간을 기록하여 성능 병목을 식별하고 최적화합니다.

### 3.2. 기록 항목

| 항목 | 설명 |
|:---|:---|
| `correlationId` | 요청 추적 ID |
| `pipelineStep` | 처리 단계명 (STT, NLP, TRANSLATE, OCR, IMAGE_GEN 등) |
| `startTime` | 단계 시작 시각 |
| `endTime` | 단계 종료 시각 |
| `processingTimeMs` | 소요시간 (밀리초) |
| `isSuccess` | 성공 여부 |

### 3.3. 구현: 파이프라인 단계 시간 측정

```java
@Slf4j
public abstract class PipelineStep<I, O> {

    private final String stepName;

    protected PipelineStep(String stepName) {
        this.stepName = stepName;
    }

    public O execute(I input) {
        MDC.put("pipelineStep", stepName);
        long startTime = System.currentTimeMillis();

        try {
            O result = doExecute(input);
            long elapsed = System.currentTimeMillis() - startTime;

            log.info("[{}] 처리 완료 - 소요시간: {}ms", stepName, elapsed);
            return result;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[{}] 처리 실패 - 소요시간: {}ms, 원인: {}", 
                      stepName, elapsed, e.getMessage(), e);
            throw e;

        } finally {
            MDC.remove("pipelineStep");
        }
    }

    protected abstract O doExecute(I input);
}
```

### 3.4. 파이프라인 전체 소요시간 로그 예시

```
[INFO] [corr:a1b2c3d4] [STT]           처리 완료 - 소요시간: 2340ms
[INFO] [corr:a1b2c3d4] [NLP]           처리 완료 - 소요시간: 1520ms
[INFO] [corr:a1b2c3d4] [TRANSLATE]     처리 완료 - 소요시간:  430ms
[INFO] [corr:a1b2c3d4] [IMAGE_GEN]     처리 완료 - 소요시간:  280ms
[INFO] [corr:a1b2c3d4] [PIPELINE_TOTAL] 처리 완료 - 총 소요시간: 4570ms
```

### 3.5. 비동기 처리 적용

장시간 걸리는 파이프라인 처리는 `@Async`를 활용하여 비동기로 실행하고, 클라이언트에게는 즉시 `correlationId`를 반환합니다.

```java
@Async("pipelineExecutor")
public CompletableFuture<PipelineResult> processAsync(PipelineRequest request) {
    MDC.put("correlationId", request.getCorrelationId());
    try {
        PipelineResult result = executePipeline(request);
        return CompletableFuture.completedFuture(result);
    } finally {
        MDC.clear();
    }
}
```

> **주의**: `@Async` 사용 시 MDC 컨텍스트가 자식 스레드로 전파되지 않으므로, 수동으로 `MDC.put()`을 호출해야 합니다.

---

## 4. NF-03: Correlation ID 기반 전 과정 추적

### 4.1. 개요

모든 API 요청에 고유한 Correlation ID를 부여하여, 요청 수신부터 응답 반환까지 파이프라인 전 과정을 단일 ID로 추적합니다.

### 4.2. Correlation ID 흐름

```
[클라이언트] ──(X-Correlation-Id 헤더)──→ [Filter]
                                            │
                                    ID 추출 또는 UUID 생성
                                    MDC.put("correlationId", id)
                                            │
                                            ▼
                        ┌─────────────────────────────────────┐
                        │  이후 모든 로그에 correlationId 자동 포함  │
                        │  STT → NLP → TRANSLATE → IMAGE_GEN  │
                        └─────────────────────────────────────┘
                                            │
                                            ▼
                                    MDC.remove("correlationId")
                                    응답 헤더에 ID 포함하여 반환
```

### 4.3. 구현: Servlet Filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String HEADER_NAME = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            // 1. 기존 헤더 확인 → 없으면 신규 생성
            String correlationId = req.getHeader(HEADER_NAME);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            // 2. MDC에 저장 → 이후 모든 로그에 자동 포함
            MDC.put(MDC_KEY, correlationId);

            // 3. 응답 헤더에 포함 (클라이언트 추적용)
            res.setHeader(HEADER_NAME, correlationId);

            chain.doFilter(request, response);

        } finally {
            // 4. 스레드 풀 오염 방지
            MDC.remove(MDC_KEY);
        }
    }
}
```

### 4.4. MDC 컨텍스트 항목

| MDC 키 | 설명 | 설정 위치 |
|:---|:---|:---|
| `correlationId` | 요청 추적 ID | CorrelationIdFilter |
| `apiKey` | 호출자 API 키 (마스킹) | 인증 인터셉터 |
| `pipelineStep` | 현재 처리 단계 | 각 PipelineStep |

### 4.5. Logback 로그 패턴 설정

#### 개발 환경 (콘솔)

```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] [corr:%X{correlationId}] [%X{pipelineStep}] %-5level %logger{30} - %msg%n</pattern>
```

#### 운영 환경 (JSON)

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>correlationId</includeMdcKeyName>
    <includeMdcKeyName>apiKey</includeMdcKeyName>
    <includeMdcKeyName>pipelineStep</includeMdcKeyName>
</encoder>
```

### 4.6. JSON 로그 출력 예시

```json
{
  "@timestamp": "2026-05-12T15:30:00.123+09:00",
  "level": "INFO",
  "logger_name": "c.a.service.SttService",
  "message": "[STT] 처리 완료 - 소요시간: 2340ms",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "apiKey": "gov_***_a1b2",
  "pipelineStep": "STT"
}
```

---

## 5. NF-05: 장애 발생 시 관리자 알림 및 에러 로그

### 5.1. 로그 레벨 정책

| 레벨 | 용도 | 운영 환경 | 예시 |
|:---|:---|:---:|:---|
| **ERROR** | 즉시 대응 필요 | ✅ | Azure API 호출 실패, DB 연결 오류 |
| **WARN** | 주의 필요 | ✅ | 재시도 발생, 캐시 미스, 임계치 접근 |
| **INFO** | 주요 이벤트 | ✅ | 파이프라인 단계 완료, API 요청/응답 |
| **DEBUG** | 디버깅 전용 | ❌ | 입력 데이터 상세, 중간 처리 결과 |

### 5.2. 에러 등급 분류 및 알림 정책

| 등급 | 조건 | 알림 방식 | 디바운싱 |
|:---|:---|:---|:---:|
| **CRITICAL** | 시스템 장애 (DB 불능, 서비스 중단) | Slack 즉시 알림 | 없음 |
| **ERROR** | Azure API 실패 (재시도 후에도 실패) | Slack 알림 | 5분 |
| **WARN** | 리소스 임계치 초과, 반복 재시도 | 대시보드 표시 | - |

### 5.3. 에러 로그 필수 포함 항목

ERROR 레벨 로그에는 반드시 아래 정보를 포함합니다.

| 항목 | 설명 |
|:---|:---|
| `correlationId` | 어떤 요청에서 발생했는지 |
| `pipelineStep` | 어떤 단계에서 발생했는지 |
| `apiKey` (마스킹) | 누구의 요청에서 발생했는지 |
| `processingTimeMs` | 실패까지 소요된 시간 |
| `errorMessage` | 오류 원인 메시지 |
| `stackTrace` | 전체 스택 트레이스 |

```java
log.error("[{}] 처리 실패 - correlationId: {}, apiKey: {}, 소요시간: {}ms",
    pipelineStep, correlationId, maskedApiKey, elapsedMs, exception);
```

### 5.4. Slack 알림 서비스

```java
@Service
@Slf4j
public class SlackAlertService {

    @Value("${slack.webhook-url}")
    private String webhookUrl;

    private final WebClient webClient;

    public SlackAlertService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /**
     * 장애 알림 전송 (NF-05)
     */
    public void sendAlert(AlertLevel level, String message, String correlationId) {
        String emoji = level == AlertLevel.CRITICAL ? "🔴" : "🟠";

        String payload = String.format("""
            {
              "blocks": [{
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "%s *[AI Cast %s]*\\n• *메시지:* %s\\n• *CorrelationId:* `%s`\\n• *시각:* %s"
                }
              }]
            }""",
            emoji, level, message, correlationId,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        webClient.post()
            .uri(webhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> log.warn("Slack 알림 전송 실패: {}", e.getMessage()))
            .subscribe();
    }
}
```

### 5.5. 전역 예외 핸들러 + 알림 연동

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final SlackAlertService slackAlertService;

    @ExceptionHandler(AzureServiceException.class)
    public ResponseEntity<ErrorResponse> handleAzureError(AzureServiceException e) {
        String correlationId = MDC.get("correlationId");

        log.error("Azure 서비스 호출 실패 - correlationId: {}", correlationId, e);

        // Slack 알림 발송 (NF-05)
        slackAlertService.sendAlert(
            AlertLevel.ERROR,
            e.getMessage(),
            correlationId
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(correlationId, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        String correlationId = MDC.get("correlationId");

        log.error("예기치 않은 오류 - correlationId: {}", correlationId, e);

        slackAlertService.sendAlert(
            AlertLevel.CRITICAL,
            "예기치 않은 시스템 오류: " + e.getClass().getSimpleName(),
            correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(correlationId, "내부 서버 오류"));
    }
}
```

---

## 6. Logback 운영 설정 (`logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- ===== 개발 환경 ===== -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] [corr:%X{correlationId}] [%X{pipelineStep}] %-5level %logger{30} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- ===== 운영 환경 ===== -->
    <springProfile name="prod">
        <!-- JSON 파일 appender -->
        <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/aicast.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/aicast.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <includeMdcKeyName>apiKey</includeMdcKeyName>
                <includeMdcKeyName>pipelineStep</includeMdcKeyName>
            </encoder>
        </appender>

        <!-- 비동기 래핑 (NF-01 비동기 처리) -->
        <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
            <queueSize>1024</queueSize>
            <discardingThreshold>0</discardingThreshold>
            <appender-ref ref="JSON_FILE"/>
        </appender>

        <root level="INFO">
            <appender-ref ref="ASYNC_FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 7. 로그 파일 관리 정책

| 항목 | 설정값 |
|:---|:---:|
| 로그 파일 위치 | `logs/aicast.log` |
| 단일 파일 최대 크기 | 100MB |
| 롤링 주기 | 일별 + 크기 기반 |
| 보존 기간 | 30일 |
| 총 용량 제한 | 3GB |
| 압축 | gzip (롤링 시 자동) |

---

## 8. 보안 고려사항

| 항목 | 처리 방식 |
|:---|:---|
| API Key | 마스킹 출력 (앞 4자 + `***` + 뒤 4자) |
| 오디오/이미지 바이너리 | 크기만 기록, 내용 미기록 |
| Azure 자격 증명 | 로그 기록 금지 |

```java
public static String maskApiKey(String apiKey) {
    if (apiKey == null || apiKey.length() <= 8) return "***";
    return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
}
```

---

**최종 업데이트**: 2026-05-12
