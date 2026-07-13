# 📋 단위테스트 수행 목록 (Unit Test List)

> **문서 ID**: UT-01  
> **분류**: 📋 절차 & 규칙  
> **목적**: AI_Cast 시스템의 각 컴포넌트별 단위테스트 대상 및 시나리오 목록 정의  
> **대상**: 모든 개발자, 에이전트  
> **작성일**: 2026-06-09  
> **최종 수정**: 2026-07-13 (v1.1)  
> **작성자**: Antigravity (AI Agent)  
> **버전**: v1.0  

---

[← 목록으로 돌아가기](../001_Self_Audit_Overview.md)

---

## 📋 목차

1. [개요](#-개요)
2. [단위테스트 대상 요약](#-단위테스트-대상-요약)
3. [상세 단위테스트 케이스 정의](#-상세-단위테스트-케이스-정의)
4. [단위테스트 실행 가이드](#-단위테스트-실행-가이드)

---

## 📌 개요

본 문서는 **AI_Cast (STT/LLM 기반 AI 오케스트레이터)** 프로젝트의 기능 안전성과 테스트 정합성을 확보하기 위한 **단위테스트 수행 목록**을 정의합니다. 
프로젝트 개발 방법론인 `ZEN_A4`에 의거하여, 핵심 비즈니스 로직을 포함한 전체 컴포넌트에 대한 단위테스트 설계 및 검증 상태를 관리합니다.

---

## 📊 단위테스트 대상 요약

| 컴포넌트 분류 | 대상 클래스 (Class) | 테스트 상태 | 테스트 방식 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| **Pipeline Service** | `DefaultPipelineService` | **완료** | JUnit / Mockito | Audio, Text 파이프라인 완료<br>Image 파이프라인 및 에러 복구 테스트 예정 |
| **Rendering Engine** | `ImageRenderingEngine` | **완료** | JUnit / Mock | 이미지 생성 및 물리적 파일 쓰기 검증 완료 |
| **Statistics** | `StatsService` | **완료** | JUnit / Mock | 일별 집계 스케줄러, 주별 통계 조회 검증 완료 |
| **Security / Auth** | `ApiKeyService` | **완료** | JUnit / Mock | API Key 유효성 검사, 권한 분기 검증 완료 |
| **Security / Auth** | `ApiKeyMasker` | **완료** | JUnit | 로그 민감 정보 마스킹 정상 치환 검증 완료 |
| **Engine / Logic** | `CategoryClassifier` | **완료** | JUnit | 키워드 기반 카테고리(재난/공지/일반) 분류 검증 완료 |
| **Engine / Logic** | `ColorSchemeMapper` | **완료** | JUnit | 카테고리별 RGB/HSL 배색 맵 매핑 검증 완료 |
| **Engine / Logic** | `FontManager` | **완료** | JUnit / Mock | NotoSans 등 다국어 폰트 로드 및 레이아웃 검증 완료 |
| **Monitoring** | `SlackAlertService` | **완료** | JUnit / Mock | Critical/Emergency 예외 알림 디바운싱 검증 완료 |
| **Monitoring** | `ResourceMonitorService`| **완료** | JUnit / Mock | 주기적 리소스 수집 및 1시간 초과 로그 삭제 검증 완료 |
| **Logging** | `ApiLogService` | **완료** | JUnit / Mock | API 호출 정보 비동기 DB 기록 검증 완료 |
| **Logging** | `AiSvcLogService` | **완료** | JUnit / Mock | AI 서비스 상세 호출 로그 비동기 DB 기록 검증 완료 |
| **Logging** | `AiSvcLogAspect` | **완료** | JUnit / Mock | AOP를 통한 호출 시간 및 결과 크기 측정 검증 완료 |
| **Client / Adapter**| `AzureSpeechClient` | **완료** | JUnit / Mock | 오디오 데이터 STT 연속 인식 및 전처리 검증 완료 |
| **Client / Adapter**| `AzureOpenAIClient` | **완료** | JUnit / Mock | dialect 표준화 및 텍스트 요약, FAST_MODE 검증 완료 |
| **Client / Adapter**| `AzureTranslatorClient`| **완료** | JUnit / Mock | 다국어 번역 및 피벗(Pivot) 번역 검증 완료 |
| **Client / Adapter**| `TranslationCacheService`| **완료** | JUnit | 번역 캐시 Hit/Miss 및 저장 검증 완료 |
| **Client / Adapter**| `AzureVisionClient` | **완료** | JUnit / Mock | 20MB 이하 이미지 OCR 텍스트 추출 검증 완료 |
| **Client / Adapter**| `AzureBlobClient` | **완료** | JUnit / Mock | 생성된 카드 뉴스 PNG 파일 업로드 및 URL 반환 검증 완료 |

---

## 📄 상세 단위테스트 케이스 정의

### 1. 파이프라인 오케스트레이션 (Pipeline Service)
*   **테스트 클래스**: `com.aicast.service.DefaultPipelineServiceTest`
*   **테스트 시나리오**:
    *   `executeAudio_Success`: 음성 수신 시 `STT` → `NLP(요약)` → `번역` → `이미지 렌더링` → `Blob 업로드` 전체 프로세스 성공 검증 (**기존**)
    *   `executeText_Success`: 텍스트 수신 시 `STT` 단계를 생략하고 `NLP(요약)` → `번역` → `이미지 렌더링` → `Blob 업로드` 정상 수행 검증 (**기존**)
    *   `executeImage_Success`: 이미지 수신 시 `OCR(텍스트 추출)` → `NLP(요약)` → `번역` → `이미지 렌더링` → `Blob 업로드` 정상 수행 검증 (**기존**)
    *   `executePipeline_ClientError`: 하위 외부 AI Client(STT, OpenAI 등) 오류 시, 비즈니스 에러로 안전하게 폴백(Fallback) 처리되고 관련 리소스가 정리되는지 검증 (**기존**)

### 2. 이미지 렌더링 엔진 (Rendering Engine)
*   **테스트 클래스**: `com.aicast.engine.ImageRenderingEngineTest`
*   **테스트 시나리오**:
    *   `renderImage_Success`: 특정 카테고리("공지"), 원문, 번역문을 입력받아 정상적으로 PNG 바이트 배열을 생성하고 임시 파일로 쓰기가 가능한지 검증 (**기존**)
    *   `renderImage_LongText`: 극단적으로 긴 텍스트 입력 시, 폰트 크기가 자동 조절되어 레이아웃 영역 밖으로 깨지지 않는지 검증 (**기존**)
    *   `renderImage_InvalidCategory`: 정의되지 않은 카테고리 입력 시 기본 테마 배색을 활용하여 예외 없이 카드를 생성하는지 검증 (**기존**)

### 3. 통계 및 스케줄러 (Statistics)
*   **테스트 클래스**: `com.aicast.service.log.StatsServiceTest`
*   **테스트 시나리오**:
    *   `aggregateDailyStats_Success`: 매일 00:10에 동작하는 스케줄러가 전날 데이터를 기반으로 DB 통계 테이블(`tb_ai_svc_stat`)에 집계(Insert/Update) SQL을 전송하는지 검증 (**기존**)
    *   `getWeeklyStats_ReturnsList`: 지자체 ID 및 날짜 범위를 제공할 때 `JdbcTemplate`을 통해 주간 통계 리스트를 정확히 매핑하여 반환하는지 검증 (**기존**)
    *   `getMonthlyStats_ReturnsList`: 월간 집계 시 올바른 파라미터가 쿼리에 바인딩되어 동작하는지 검증 (**기존**)

### 4. 보안 및 인증 (Security / Auth)
*   **테스트 클래스**: `com.aicast.security.ApiKeyServiceTest` (**기존**)
*   **테스트 시나리오**:
    *   `isValidKey_ValidKey`: 데이터베이스(`gov_list`)에 존재하는 활성 API Key 조회 시 `true` 반환 검증
    *   `isValidKey_InvalidKey`: 존재하지 않는 무효한 Key 조회 시 `false` 반환 검증
    *   `isAdmin_AdminUser`: 관리자 권한 플래그가 지정된 Key 조회 시 `true` 반환 검증
    *   `isAdmin_GeneralUser`: 일반 지자체 사용자 Key 조회 시 `false` 반환 검증

### 5. API Key 마스킹 (Security / Masker)
*   **테스트 클래스**: `com.aicast.common.util.ApiKeyMaskerTest` (**기존**)
*   **테스트 시나리오**:
    *   `mask_StandardKey`: 30자 이상의 API Key 문자열이 입력되었을 때 앞 4자리와 뒤 4자리를 제외한 본문을 `*`로 올바르게 치환하는지 검증
    *   `mask_ShortKey`: 길이가 8자 미만으로 짧은 문자열 입력 시 전체 마스킹 처리 혹은 예외 처리가 정상적으로 이루어지는지 검증
    *   `mask_NullOrEmpty`: Null 혹은 공백 입력 시 에러 없이 빈 값을 반환하는 강인성 검증

### 6. 카테고리 분류 및 테마 배색 (Engine / Logic)
*   **테스트 클래스**: `com.aicast.engine.CategoryClassifierTest` & `ColorSchemeMapperTest` (**기존**)
*   **테스트 시나리오**:
    *   `classify_DisasterKeywords`: "호우 경보", "지진 대피", "태풍 주의보" 등의 핵심 키워드가 원문에 있을 때 "재난" 카테고리로 매핑되는지 검증
    *   `classify_InfoKeywords`: "주민 자치 센터", "체육 대회", "신청 접수" 등 행정 정보 키워드가 있을 때 "공지/안내" 카테고리로 매핑되는지 검증
    *   `getColorScheme_CategoryMatch`: "재난" 카테고리에는 고대비 빨간색(Red/Orange) 테마, "공지" 카테고리에는 신뢰감을 주는 파란색(Blue/Green) 테마의 RGB/HSL 값 세트가 정확히 할당되는지 검증

### 7. 다국어 폰트 및 레이아웃 관리 (Engine / Font)
*   **테스트 클래스**: `com.aicast.engine.FontManagerTest` (**기존**)
*   **테스트 시나리오**:
    *   `getFont_KoreanEnglish`: 기본 국/영문 폰트 로드 성공 검증
    *   `getFont_MultiLanguage`: 베트남어, 러시아어, 태국어 등 특수 글리프(Glyph)가 포함된 다국어 폰트가 텍스트 렌더링 시 깨지지 않고 NotoSans 다국어 폰트로 정상 로드되는지 검증
    *   `getFont_MissingFontFallback`: 로컬 시스템에 특정 폰트가 없을 때 JVM 기본 물리 폰트(SansSerif 등)로 폴백(Fallback)하여 에러가 전파되지 않도록 조치되었는지 검증

### 8. 알림 디바운싱 및 리소스 모니터링 (Monitoring)
*   **테스트 클래스**: `com.aicast.service.alert.SlackAlertServiceTest` & `ResourceMonitorServiceTest` (**기존**)
*   **테스트 시나리오**:
    *   `sendAlert_Emergency`: Emergency 등급 장애의 경우 지연 없이 슬랙 Webhook을 즉시 호출하는지 검증 (**기존**)
    *   `sendAlert_CriticalDebounce`: Critical 등급 장애가 5초 간격으로 연속 10번 발생할 때, 디바운싱 필터링을 거쳐 60초 내에는 단 1번만 외부 Webhook으로 알림을 전송하는지 검증 (**기존**)
    *   `collectResources_Scheduled`: 5초 주기로 시스템의 리소스(CPU 사용률, 메모리, 네트워크 송수신량 등)를 수집하여 DB 엔티티로 적재 처리하는지 검증 (**기존**)
    *   `cleanupOldLogs_Scheduled`: 1시간 초과된 오래된 리소스 로그를 자동 삭제하는 스케줄러가 정상 동작하는지 검증 (**기존**)
    *   `collectResources_NullDataHandling`: 리소스 수집 실패(null 데이터) 시에도 예외가 전파되지 않고 안전하게 처리되는지 검증 (**기존**)

### 9. 로깅 서비스 (Logging)
*   **테스트 클래스**: `com.aicast.service.log.ApiLogServiceTest` & `AiSvcLogServiceTest` (**기존**)
*   **테스트 시나리오**:
    *   `saveLog_Success`: API 호출 정보를 비동기로 DB에 정상 기록하는지 검증 (**기존**)
    *   `saveLog_ExceptionHandled`: DB 연결 실패 시에도 예외가 전파되지 않고 안전하게 처리되는지 검증 (**기존**)

### 10. AOP 로깅 (AOP Logging)
*   **테스트 클래스**: `com.aicast.common.aop.AiSvcLogAspectTest` (**기존**)
*   **테스트 시나리오**:
    *   `logAiServiceCall_Success`: STT, NLP, TRANSLATE, OCR, STORAGE 각 서비스 호출 시 정상적으로 로그가 기록되는지 검증 (**기존**)
    *   `logAiServiceCall_Exception`: 서비스 호출 중 예외 발생 시에도 로그가 기록되는지 검증 (**기존**)

### 11. 캐싱 서비스 (Translation Cache)
*   **테스트 클래스**: `com.aicast.client.translate.TranslationCacheServiceTest` (**기존**)
*   **테스트 시나리오**:
    *   `getTranslation_CacheHit`: 이미 번역한 적이 있는 텍스트와 대상 언어의 조합 요청 시, Azure Translator API를 호출하지 않고 캐시 메모리에서 즉시 데이터를 반환하는지 검증
    *   `getTranslation_CacheMiss`: 캐시에 존재하지 않는 문장의 경우 API 통신을 통해 신규 번역을 수행한 뒤 캐시에 저장하는지 검증

---

## 🚀 단위테스트 실행 가이드

프로젝트 루트 디렉토리(`AI_Cast`)에서 아래 명령어를 사용하여 전체 단위테스트 또는 특정 테스트를 골라 실행할 수 있습니다.

### 1. 전체 단위테스트 실행
```bash
mvn clean test
```

### 2. 특정 테스트 클래스 실행
```bash
mvn test -Dtest=DefaultPipelineServiceTest
```

### 3. 특정 테스트 메서드 실행
```bash
mvn test -Dtest=DefaultPipelineServiceTest#executeAudio_Success
```

### 4. 테스트 커버리지 리포트 생성 (JaCoCo 설정 활성화 시)
```bash
mvn clean test jacoco:report
```
*   생성 경로: `target/site/jacoco/index.html`

---

[← 목록으로 돌아가기](../001_Self_Audit_Overview.md)
