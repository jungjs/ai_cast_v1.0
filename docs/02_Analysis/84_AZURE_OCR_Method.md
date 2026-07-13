# Azure AI Vision OCR 활용방안

본 문서는 AI Cast 시스템의 이미지-텍스트 변환(OCR) 요구사항(F-05)에 대한 Azure AI Vision 서비스 활용방안을 정리합니다.

---

## 1. Azure AI Vision OCR 서비스 개요

Azure AI Vision Image Analysis 4.0의 Read OCR API는 딥러닝 기반으로 이미지에서 텍스트를 추출하는 고급 서비스입니다.

### 1.1. 핵심 기능

| 기능 | 설명 |
|:---|:---|
| **인쇄 텍스트 인식** | 164개 이상 언어의 인쇄 텍스트 추출 (한국어 포함) |
| **필기 텍스트 인식** | 주요 언어의 필기체 인식 지원 |
| **동기 API** | v4.0은 동기 방식으로 즉시 결과 반환 |
| **바운딩 폴리곤** | 텍스트 위치 좌표 및 신뢰도 점수 제공 |
| **다국어 혼합** | 단일 이미지 내 인쇄·필기 혼합 및 다국어 혼합 지원 |

### 1.2. Azure AI Vision vs Document Intelligence

| 항목 | AI Vision (Read OCR) ⭐ | Document Intelligence |
|:---|:---|:---|
| **용도** | 일반 이미지 (사진, 스크린샷, 라벨) | 문서 (PDF, 스캔, 양식) |
| **처리 방식** | 동기 (즉시 응답) | 비동기 (폴링/웹훅) |
| **AI Cast 적합도** | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **비고** | JPG 이미지 입력에 최적 | 복잡한 문서 구조 분석에 최적 |

> **권장**: AI Cast의 F-05는 JPG 이미지 입력이므로, **Azure AI Vision Read OCR**이 적합합니다.

---

## 2. 지원 사양

### 2.1. 이미지 포맷

| 항목 | 사양 |
|:---|:---|
| **지원 포맷** | JPEG, PNG, GIF, BMP, TIFF, WEBP, ICO, MPO |
| **최대 파일 크기** | 20 MB |
| **최소 크기** | 50 × 50 픽셀 |
| **최대 크기** | 16,000 × 16,000 픽셀 |

### 2.2. 한국어 인식

| 항목 | 내용 |
|:---|:---|
| **지원 여부** | ✅ 한국어 인쇄 텍스트 지원 |
| **정확도** | 선명한 인쇄 텍스트에서 높은 정확도 |
| **정확도 영향 요소** | 해상도, 대비, 조명, 텍스트 방향 |
| **필기 인식** | 제한적 지원 (인쇄 대비 낮은 정확도) |

---

## 3. API 호출 예시

### 3.1. REST API 요청

```bash
POST {endpoint}/computervision/imageanalysis:analyze?api-version=2024-02-01&features=read
Headers:
  Ocp-Apim-Subscription-Key: <API_KEY>
  Content-Type: application/octet-stream

Body: <이미지 바이너리 데이터>
```

### 3.2. 응답 구조

```json
{
  "readResult": {
    "blocks": [{
      "lines": [
        {
          "text": "오늘 부산 지역에 폭우 주의보",
          "boundingPolygon": [
            {"x": 10, "y": 5}, {"x": 350, "y": 5},
            {"x": 350, "y": 35}, {"x": 10, "y": 35}
          ],
          "words": [
            {"text": "오늘", "confidence": 0.98},
            {"text": "부산", "confidence": 0.99},
            {"text": "지역에", "confidence": 0.97},
            {"text": "폭우", "confidence": 0.96},
            {"text": "주의보", "confidence": 0.99}
          ]
        }
      ]
    }]
  }
}
```

---

## 4. Spring Boot 통합 구현

### 4.1. Maven 의존성

```xml
<dependencies>
    <!-- Azure AI Vision SDK -->
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-ai-vision-imageanalysis</artifactId>
        <version>1.0.0-beta.1</version>
    </dependency>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
        <version>1.11.0</version>
    </dependency>
</dependencies>
```

### 4.2. 설정 파일 (`application.yml`)

```yaml
azure:
  vision:
    key: ${AZURE_VISION_KEY}
    endpoint: ${AZURE_VISION_ENDPOINT}   # 예: https://<resource>.cognitiveservices.azure.com
    
    # OCR 설정
    ocr:
      language: ko                        # 기본 인식 언어
      min-confidence: 0.7                 # 최소 신뢰도 필터
```

### 4.3. 서비스 클래스 설계

#### 방식 A: Azure AI Vision SDK 활용 (권장)

```java
@Service
@Slf4j
public class OcrService {
    
    private final ImageAnalysisClient client;
    private final double minConfidence;
    
    public OcrService(
            @Value("${azure.vision.endpoint}") String endpoint,
            @Value("${azure.vision.key}") String key,
            @Value("${azure.vision.ocr.min-confidence}") double minConfidence) {
        this.client = new ImageAnalysisClientBuilder()
            .endpoint(endpoint)
            .credential(new KeyCredential(key))
            .buildClient();
        this.minConfidence = minConfidence;
    }
    
    /**
     * 이미지에서 텍스트 추출 (F-05)
     * @param imageFile JPG 이미지 파일
     * @return 추출된 텍스트
     */
    public OcrResult extractText(MultipartFile imageFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            byte[] imageBytes = imageFile.getBytes();
            
            // 이미지 분석 수행 (READ 기능만 사용)
            ImageAnalysisResult result = client.analyze(
                BinaryData.fromBytes(imageBytes),
                Arrays.asList(VisualFeatures.READ),
                null  // ImageAnalysisOptions
            );
            
            // 텍스트 추출 및 신뢰도 필터링
            StringBuilder extractedText = new StringBuilder();
            List<WordInfo> words = new ArrayList<>();
            
            if (result.getRead() != null) {
                for (var block : result.getRead().getBlocks()) {
                    for (var line : block.getLines()) {
                        // 라인별 평균 신뢰도 계산
                        double avgConfidence = line.getWords().stream()
                            .mapToDouble(w -> w.getConfidence())
                            .average().orElse(0.0);
                        
                        if (avgConfidence >= minConfidence) {
                            extractedText.append(line.getText()).append("\n");
                        }
                    }
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("OCR 처리 완료 - 소요시간: {}ms", elapsed);
            
            return new OcrResult(
                extractedText.toString().trim(),
                elapsed, "SUCCESS"
            );
            
        } catch (Exception e) {
            log.error("OCR 처리 실패: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return new OcrResult(null, elapsed, "FAILED: " + e.getMessage());
        }
    }
}
```

#### 방식 B: REST API 직접 호출

```java
@Service
@Slf4j
public class OcrRestService {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public OcrRestService(
            WebClient.Builder builder,
            @Value("${azure.vision.endpoint}") String endpoint,
            @Value("${azure.vision.key}") String key) {
        this.webClient = builder.baseUrl(endpoint).build();
        this.apiKey = key;
    }
    
    public String extractText(byte[] imageBytes) {
        String response = webClient.post()
            .uri("/computervision/imageanalysis:analyze?api-version=2024-02-01&features=read")
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(imageBytes)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        return parseReadResult(response);
    }
}
```

### 4.4. 응답 DTO

```java
@Data
@AllArgsConstructor
public class OcrResult {
    private String extractedText;      // 추출된 텍스트
    private long processingTimeMs;     // 처리 소요시간
    private String status;             // SUCCESS / FAILED
}
```

---

## 5. 이미지 전처리

입력 이미지의 품질에 따라 OCR 정확도가 크게 달라지므로, 전처리를 수행합니다.

### 5.1. 전처리 파이프라인

```
입력 JPG → 포맷 검증 → 크기 확인 → (필요 시) 리사이즈 → OCR 호출
```

### 5.2. 전처리 서비스

```java
@Service
public class ImagePreprocessor {
    
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;  // 20MB
    private static final int MIN_DIMENSION = 50;
    private static final int MAX_DIMENSION = 16000;
    
    /**
     * 이미지 검증 및 전처리
     */
    public byte[] preprocess(MultipartFile imageFile) throws Exception {
        // 1. 파일 크기 검증
        if (imageFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지 크기가 20MB를 초과합니다.");
        }
        
        // 2. 이미지 포맷 검증 (JPG만 허용 - F-05)
        String contentType = imageFile.getContentType();
        if (!"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("JPG 포맷만 지원합니다.");
        }
        
        // 3. 이미지 크기 확인 및 리사이즈
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        if (image.getWidth() < MIN_DIMENSION || image.getHeight() < MIN_DIMENSION) {
            throw new IllegalArgumentException("이미지가 너무 작습니다 (최소 50x50).");
        }
        
        // 4. 대용량 이미지 리사이즈 (선택)
        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            image = resizeImage(image, MAX_DIMENSION);
        }
        
        return toByteArray(image, "jpg");
    }
}
```

### 5.3. OCR 정확도 향상 팁

| 항목 | 권장 사항 |
|:---|:---|
| **해상도** | 최소 150 DPI 이상, 300 DPI 권장 |
| **대비** | 배경과 텍스트의 명확한 대비 확보 |
| **텍스트 방향** | 가능한 수평 정렬, 기울어진 이미지는 보정 후 전송 |
| **노이즈** | 흐릿한 이미지는 샤프닝 필터 적용 |
| **조명** | 균일한 조명 확보, 그림자 최소화 |

---

## 6. 오류 처리

### 6.1. 주요 오류 유형

| HTTP 코드 | 원인 | 대응 |
|:---:|:---|:---|
| 400 | 잘못된 이미지 포맷/크기 | 전처리에서 사전 검증 |
| 401 | 인증 실패 | API 키 확인 |
| 415 | 지원하지 않는 미디어 타입 | Content-Type 헤더 확인 |
| 429 | 요청 제한 초과 | 지수 백오프 재시도 |
| 500 | 서비스 내부 오류 | 재시도 |

### 6.2. 재시도 전략

```java
@Retryable(
    retryFor = {HttpServerErrorException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2)
)
public OcrResult extractTextWithRetry(MultipartFile imageFile) {
    return extractText(imageFile);
}
```

---

## 7. 비용 정보

### 7.1. 요금 체계

| 티어 | 요금 | 제한 |
|:---|:---:|:---|
| **무료 (F0)** | 무료 | 월 5,000건, 분당 20건 |
| **표준 (S1)** | ~$1.00 / 1,000건 | 초당 10건 (증설 가능) |

### 7.2. AI Cast 월간 비용 예측

| 시나리오 | 일일 처리 건수 | 월간 건수 | 월간 예상 비용 |
|:---|:---:|:---:|:---:|
| 소규모 | 50건 | 1,500건 | 무료 (F0 범위) |
| 중규모 | 200건 | 6,000건 | ~$6/월 |
| 대규모 | 1,000건 | 30,000건 | ~$30/월 |

---

## 8. AI Cast 파이프라인 내 OCR 위치

```
┌──────────────┐     ┌──────────────┐     ┌─────────┐     ┌────────────┐
│ JPG 이미지   │ ──→ │ 전처리       │ ──→ │ OCR     │ ──→ │ NLP        │ ──→ Translation → ...
│ 입력 (F-05)  │     │ 검증/리사이즈│     │ (본문서) │     │ 정제+요약  │
└──────────────┘     └──────────────┘     └─────────┘     └────────────┘
```

### 8.1. 파이프라인 내 OCR 역할

1. **입력**: 전처리된 JPG 이미지 데이터
2. **처리**: Azure AI Vision Read OCR로 이미지 → 텍스트 변환
3. **출력**: 추출된 한국어 텍스트 (NLP 정제+요약 단계로 전달)
4. **로깅**: OCR 호출 로그 기록 (F-11 연계)

---

## 9. 요구사항 매핑

| 요구사항 ID | 설명 | 관련 섹션 |
|:---:|:---|:---|
| F-05 | 이미지 처리 파이프라인 (image to text 변환) | §4.3 서비스 클래스, §8 파이프라인 |
| F-11 | OCR 호출 로그 기록 | §4.3 서비스 내 로깅 |

---

**최종 업데이트**: 2026-05-12  
**참조**: [Azure AI Vision 공식 문서](https://learn.microsoft.com/en-us/azure/ai-services/computer-vision/overview-ocr)
