# Azure Speech-to-Text (STT) AI 활용방안

본 문서는 AI Cast 시스템의 음성 처리 요구사항(F-03, F-20, F-21)에 대한 Azure Speech-to-Text 서비스의 활용방안을 조사·정리합니다.

---

## 1. Azure Speech-to-Text 서비스 개요

Azure AI Speech 서비스는 오디오 스트림을 텍스트로 변환하는 고급 음성 인식 기능을 제공합니다.

### 1.1. 핵심 기능 (Core Features)

| 기능 | 설명 | 적합 시나리오 |
|:---|:---|:---|
| **실시간 전사 (Real-time Transcription)** | 마이크 또는 파일에서 오디오를 실시간으로 인식하여 즉시 텍스트로 변환 | 실시간 자막, 음성 명령, 대화형 시스템 |
| **빠른 전사 (Fast Transcription)** | 오디오 파일을 동기 방식으로 실시간보다 빠르게 전사 | 빠른 오디오 파일 전사, 회의록, 음성 메일 |
| **일괄 전사 (Batch Transcription)** | 대량의 녹음 오디오를 비동기 방식으로 효율적 처리 | 대량 오디오 파일 처리, 콜센터 분석 |
| **사용자 지정 음성 (Custom Speech)** | 특정 도메인/환경에 맞게 음성 인식 정확도를 향상시킨 커스텀 모델 | 전문 용어, 방언, 특수 환경 |

### 1.2. 추가 기능

| 기능 | 설명 |
|:---|:---|
| **화자 분리 (Diarization)** | 오디오 내 최대 35명의 서로 다른 화자를 구분·분리 |
| **구문 목록 (Phrase Lists)** | 특정 단어/구문을 사전 제공하여 인식 정확도 향상 (고유명사, 전문 용어) |
| **언어 감지 (Language Detection)** | 오디오 입력 언어를 자동 식별하여 적절한 인식 알고리즘 적용 |

---

## 2. AI Cast 시스템 적용 방안

### 2.1. 적용 방식 비교 및 선정

AI Cast 시스템의 `/api/process_audio` 파이프라인(F-03)에 적합한 방식을 비교합니다.

| 방식 | SDK (Real-time) | SDK (Continuous) | REST API (Fast) | REST API (Batch) |
|:---|:---:|:---:|:---:|:---:|
| **처리 방식** | 동기 (단일 발화) | 이벤트 기반 (연속) | 동기 (HTTP) | 비동기 (HTTP) |
| **오디오 길이** | 짧은 오디오 (< 15초) | 긴 오디오 (무제한) | 중간 길이 | 대용량 |
| **결과 반환** | 즉시 | 이벤트 콜백 | 즉시 | 폴링/웹훅 |
| **지연시간** | 매우 낮음 | 낮음 | 낮음 | 높음 (분~시간) |
| **Java SDK 지원** | ✅ | ✅ | ✅ | ✅ |
| **Spring Boot 통합** | 용이 | 보통 | 매우 용이 | 용이 |
| **AI Cast 적합도** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

> **권장**: AI Cast 시스템은 지역 방송 음성 데이터(WAV)를 처리하므로, 오디오 길이가 다양할 수 있습니다.
> - **1차 권장**: **SDK Continuous Recognition** — 다양한 길이의 오디오를 안정적으로 처리
> - **2차 권장**: **Fast Transcription API** — 짧은 오디오에 대해 간결한 REST 기반 처리

### 2.2. 접근 방식별 상세 설명

#### 방식 A: SDK — `recognizeOnceAsync()` (단일 발화 인식)

**용도**: 15초 이하의 짧은 오디오 클립

```java
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;

public String recognizeShortAudio(String audioFilePath) throws Exception {
    SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
    speechConfig.setSpeechRecognitionLanguage("ko-KR");  // 한국어 설정
    
    AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFilePath);
    
    try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {
        SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get();
        
        if (result.getReason() == ResultReason.RecognizedSpeech) {
            return result.getText();
        } else if (result.getReason() == ResultReason.NoMatch) {
            throw new RuntimeException("음성을 인식할 수 없습니다.");
        } else if (result.getReason() == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(result);
            throw new RuntimeException("STT 취소: " + cancellation.getErrorDetails());
        }
    }
    return null;
}
```

- **장점**: 구현이 단순, 동기 방식으로 결과 즉시 반환
- **단점**: 15초 초과 오디오 처리 불가, 첫 번째 발화만 인식

---

#### 방식 B: SDK — `startContinuousRecognitionAsync()` (연속 인식) ⭐ 권장

**용도**: 길이에 관계없이 전체 오디오 파일 처리

```java
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;

public String recognizeLongAudio(String audioFilePath) throws Exception {
    SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
    speechConfig.setSpeechRecognitionLanguage("ko-KR");
    
    AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFilePath);
    Semaphore stopSemaphore = new Semaphore(0);
    List<String> recognizedTexts = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    
    try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {
        
        // 중간 결과 (실시간 피드백용, 선택 사항)
        recognizer.recognizing.addEventListener((s, e) -> {
            // 부분 인식 결과 (로깅 등에 활용)
        });
        
        // 최종 인식 결과
        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                recognizedTexts.add(e.getResult().getText());
            }
        });
        
        // 오류 처리
        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                errors.add("STT Error: " + e.getErrorDetails());
            }
            stopSemaphore.release();
        });
        
        // 세션 종료 시 대기 해제
        recognizer.sessionStopped.addEventListener((s, e) -> {
            stopSemaphore.release();
        });
        
        // 연속 인식 시작
        recognizer.startContinuousRecognitionAsync().get();
        
        // 오디오 파일 처리 완료 대기
        stopSemaphore.acquire();
        
        // 연속 인식 중지
        recognizer.stopContinuousRecognitionAsync().get();
    }
    
    if (!errors.isEmpty()) {
        throw new RuntimeException(String.join("; ", errors));
    }
    
    return String.join(" ", recognizedTexts);
}
```

- **장점**: 오디오 길이 제한 없음, 이벤트 기반으로 대용량 오디오 안정 처리
- **단점**: 비동기 이벤트 패턴으로 구현 복잡도 증가

---

#### 방식 C: Fast Transcription REST API

**용도**: 중간 길이 오디오의 빠른 동기 처리

```java
// Fast Transcription API는 REST 기반으로 HTTP 요청으로 호출
// Spring Boot의 RestTemplate 또는 WebClient 활용

public String fastTranscribe(byte[] audioData) {
    String endpoint = String.format(
        "https://%s.api.cognitive.microsoft.com/speechtotext/transcriptions:transcribe?api-version=2024-11-15",
        region
    );
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
    // Multipart 요청 구성
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("audio", new ByteArrayResource(audioData) {
        @Override
        public String getFilename() { return "audio.wav"; }
    });
    body.add("definition", "{\"locales\":[\"ko-KR\"]}");
    
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);
    
    return response.getBody(); // JSON 응답에서 텍스트 추출
}
```

- **장점**: REST 기반으로 구현 간단, 별도 SDK 불필요
- **단점**: SDK 대비 세밀한 제어 어려움
- **지원 포맷**: WAV, MP3, OPUS/OGG, FLAC, WMA, AAC, M4A 등

---

## 3. 오디오 포맷 요구사항 및 전처리

### 3.1. 지원 오디오 포맷

| 구분 | 표준 STT (SDK) | Fast Transcription API |
|:---|:---|:---|
| **필수 포맷** | WAV (16-bit PCM) | WAV, MP3, OPUS/OGG, FLAC 등 다양 |
| **권장 샘플레이트** | 16 kHz | 16 kHz |
| **권장 채널** | Mono | Mono |
| **인코딩** | 16-bit PCM (비압축) | PCM, ALAW, MULAW 등 |

### 3.2. 오디오 전처리 (F-21 관련)

AI Cast 시스템의 요구사항 F-21에 따라, 음성 인식 최적화를 위한 자동 전처리를 수행합니다.

```java
/**
 * 오디오 전처리 서비스
 * - WAV 포맷 검증 및 변환
 * - 샘플레이트 변환 (16kHz)
 * - 모노 채널 변환
 */
@Service
public class AudioPreprocessor {
    
    /**
     * WAV 파일 포맷 검증
     */
    public AudioFormatInfo validateWavFormat(InputStream audioStream) {
        // WAV 헤더 파싱 (RIFF 구조 확인)
        // - 샘플레이트 확인 (8kHz 또는 16kHz 권장)
        // - 비트 깊이 확인 (16-bit 권장)
        // - 채널 수 확인 (Mono 권장)
        return audioFormatInfo;
    }
    
    /**
     * 필요 시 포맷 변환 수행
     * - javax.sound.sampled API 또는 FFmpeg 활용
     */
    public byte[] convertToOptimalFormat(byte[] audioData) {
        // 1. 샘플레이트가 16kHz가 아닌 경우 리샘플링
        // 2. 스테레오인 경우 모노로 변환
        // 3. 비트 깊이가 16-bit가 아닌 경우 변환
        return convertedAudioData;
    }
}
```

**전처리 파이프라인**:

```
입력 WAV → 포맷 검증 → 리샘플링(16kHz) → 모노 변환 → 16-bit PCM 변환 → STT 호출
```

---

## 4. Spring Boot 통합 아키텍처

### 4.1. Maven 의존성

```xml
<dependencies>
    <!-- Azure Speech SDK -->
    <dependency>
        <groupId>com.microsoft.cognitiveservices.speech</groupId>
        <artifactId>client-sdk</artifactId>
        <version>1.48.1</version>  <!-- 최신 안정 버전 사용 -->
    </dependency>
</dependencies>
```

### 4.2. 설정 파일 (`application.yml`)

```yaml
azure:
  speech:
    key: ${AZURE_SPEECH_KEY}       # 환경 변수에서 로드
    region: ${AZURE_SPEECH_REGION}  # 예: koreacentral
    language: ko-KR                 # 기본 인식 언어
    
    # 오디오 전처리 설정
    preprocessing:
      target-sample-rate: 16000     # 16kHz
      target-bit-depth: 16          # 16-bit
      target-channels: 1            # Mono
```

### 4.3. 서비스 클래스 설계

```java
@Service
@Slf4j
public class AzureSttService {
    
    private final String subscriptionKey;
    private final String region;
    private final String language;
    private final AudioPreprocessor preprocessor;
    
    public AzureSttService(
            @Value("${azure.speech.key}") String key,
            @Value("${azure.speech.region}") String region,
            @Value("${azure.speech.language}") String language,
            AudioPreprocessor preprocessor) {
        this.subscriptionKey = key;
        this.region = region;
        this.language = language;
        this.preprocessor = preprocessor;
    }
    
    /**
     * 오디오 파일을 텍스트로 변환 (F-03, F-20)
     * 연속 인식 방식으로 긴 오디오도 처리 가능
     */
    public SttResult transcribe(MultipartFile audioFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 오디오 전처리 (F-21)
            byte[] preprocessedAudio = preprocessor.convertToOptimalFormat(
                audioFile.getBytes()
            );
            
            // 2. 임시 파일로 저장 (SDK는 파일 경로 필요)
            Path tempFile = saveTempWavFile(preprocessedAudio);
            
            // 3. STT 수행
            String text = performContinuousRecognition(tempFile.toString());
            
            // 4. 결과 반환
            long elapsed = System.currentTimeMillis() - startTime;
            return new SttResult(text, elapsed, "SUCCESS");
            
        } catch (Exception e) {
            log.error("STT 처리 실패: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return new SttResult(null, elapsed, "FAILED: " + e.getMessage());
        }
    }
    
    private String performContinuousRecognition(String filePath) throws Exception {
        SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, region);
        config.setSpeechRecognitionLanguage(language);
        
        // 구문 목록 설정 (지역 방송 관련 키워드)
        PhraseListGrammar phraseList = PhraseListGrammar.fromRecognizer(recognizer);
        phraseList.addPhrase("재난방송");
        phraseList.addPhrase("긴급속보");
        // ... 도메인 특화 키워드 추가
        
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(filePath);
        // ... 연속 인식 로직 (방식 B 참조)
    }
}
```

### 4.4. 핵심 설계 원칙

| 원칙 | 설명 |
|:---|:---|
| **SpeechRecognizer 비공유** | 요청마다 새 인스턴스 생성 (스레드 안전성 확보) |
| **리소스 정리** | `try-with-resources` 사용으로 네이티브 리소스 반드시 해제 |
| **비동기 오류 처리** | `canceled` 이벤트 핸들러에서 오류 감지 및 로깅 |
| **자격 증명 보안** | 환경 변수 또는 Azure Key Vault 사용, 코드에 하드코딩 금지 |
| **재시도 로직** | 일시적 네트워크 오류/쓰로틀링(429) 시 지수 백오프 재시도 |

---

## 5. 오류 처리 및 재시도 전략

### 5.1. 주요 오류 유형

| 오류 유형 | 원인 | 대응 방안 |
|:---|:---|:---|
| `NoMatch` | 음성 인식 불가 (무음, 노이즈) | 오디오 품질 확인 후 사용자에게 알림 |
| `Canceled - Error` | 인증 실패, 네트워크 오류 | 자격 증명 확인, 재시도 |
| `Canceled - EndOfStream` | 오디오 스트림 정상 종료 | 정상 완료 처리 |
| HTTP 429 | 요청 쓰로틀링 (과다 호출) | 지수 백오프 재시도 |
| `UnsatisfiedLinkError` | 네이티브 라이브러리 로드 실패 | SDK 의존성 및 OS 호환성 확인 |

### 5.2. 재시도 전략 (Spring Retry 활용)

```java
@Retryable(
    retryFor = {TransientSttException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public SttResult transcribeWithRetry(MultipartFile audioFile) {
    return transcribe(audioFile);
}
```

---

## 6. 가격 정보

### 6.1. 요금 체계

| 서비스 유형 | 예상 가격 (오디오 시간당) | 비고 |
|:---|:---:|:---|
| **무료 티어 (F0)** | 무료 | 월 5시간 제한, Batch 미지원 |
| **표준 실시간 전사** | ~$1.00 | Pay-as-you-go |
| **일괄 전사 (Batch)** | ~$0.18 ~ $0.36 | 대용량 처리에 경제적 |
| **사용자 지정 실시간** | ~$1.20 | Custom Speech 모델 사용 시 |
| **사용자 지정 일괄** | ~$0.225 ~ $0.45 | Custom Speech + Batch |

> **참고**: 정확한 가격은 [Azure Speech 서비스 가격 페이지](https://azure.microsoft.com/pricing/details/cognitive-services/speech-services/)에서 확인하세요.
> 초 단위 과금이며, 리전에 따라 가격이 다를 수 있습니다.

### 6.2. AI Cast 비용 예측 시나리오

| 시나리오 | 일일 처리량 | 월간 예상 비용 |
|:---|:---:|:---:|
| **소규모** | 1시간/일 | ~$30/월 |
| **중규모** | 5시간/일 | ~$150/월 |
| **대규모** | 20시간/일 | ~$600/월 |

---

## 7. 한국어(ko-KR) 관련 고려사항

### 7.1. 한국어 인식 최적화

| 항목 | 권장 사항 |
|:---|:---|
| **언어 코드** | `ko-KR` 설정 필수 |
| **사투리 처리** | 기본 모델은 표준어 기반 → Custom Speech로 방언 정확도 향상 가능 |
| **구문 목록** | 지역 방송 관련 고유명사, 지명, 전문 용어를 Phrase List에 등록 |
| **오디오 품질** | 방송 오디오는 일반적으로 고품질이므로 추가 노이즈 제거 불필요 |

### 7.2. Custom Speech 모델 활용 (선택 사항)

지역 방송 사투리 인식 정확도가 부족한 경우, Custom Speech 모델을 구축할 수 있습니다.

```
학습 데이터 준비 → Custom Speech 프로젝트 생성 → 모델 학습 → 배포 → 엔드포인트 설정
```

- **학습 데이터**: 지역 방송 오디오 + 정확한 전사 텍스트 쌍
- **최소 데이터**: 텍스트 데이터만으로도 가능 (도메인 용어 학습)
- **추가 비용**: 학습 시간당 별도 과금 + 엔드포인트 호스팅 비용

---

## 8. AI Cast 파이프라인 내 STT 위치

```
┌─────────────┐     ┌──────────────┐     ┌─────────┐     ┌────────────┐     ┌──────────────┐
│ WAV 오디오   │ ──→ │ 전처리(F-21) │ ──→ │ STT     │ ──→ │ NLP        │ ──→ │ Translation  │ ──→ ...
│ 입력 (F-03) │     │ 포맷 변환    │     │ (F-20)  │     │ (F-22 등)  │     │              │
└─────────────┘     └──────────────┘     └─────────┘     └────────────┘     └──────────────┘
```

### 8.1. 파이프라인 내 STT 역할

1. **입력**: 전처리된 WAV 오디오 데이터
2. **처리**: Azure Speech SDK를 통한 음성-텍스트 변환
3. **출력**: 인식된 한국어 텍스트 (NLP 단계로 전달)
4. **로깅**: STT 호출 로그 기록 (F-11, F-12 연계)

---

## 9. 요구사항 매핑

| 요구사항 ID | 설명 | 관련 섹션 |
|:---:|:---|:---|
| F-03 | 음성 처리 파이프라인 API | §2 적용 방안, §8 파이프라인 |
| F-20 | Azure Speech SDK를 이용한 오디오-텍스트 변환 | §2.2 방식 B (연속 인식), §4.3 서비스 클래스 |
| F-21 | 음성 인식 최적화를 위한 오디오 포맷 자동 전처리 | §3 오디오 포맷 및 전처리 |
| F-11 | STT 호출 로그 기록 | §4.3 서비스 클래스 내 로깅 |

---

**최종 업데이트**: 2026-05-12  
**참조**: [Azure Speech-to-Text 공식 문서](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-to-text)
