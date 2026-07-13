# Phase 2: Azure AI 클라이언트 및 파이프라인 개발 작업 로그

**작성일시**: 2026-05-12
**작업자**: Antigravity AI Assistant
**기능명**: Azure 멀티모달 파이프라인 (STT, NLP, OCR, Translate, Blob) 및 렌더링 엔진
**관련 WBS**: WBS-01 (2.1, 2.2, 2.3)

---

## 1. 적용 체크리스트
- `PHASE_2_EXECUTE_CHECKLIST` (외부 API 연동, 에러 핸들링, 비동기 타임아웃 검토)

---

## 2. 작업 내역 및 결과

### 2.1 Azure AI 클라이언트 연동
- **내용**: Azure SDK 및 WebClient를 이용하여 인터페이스와 구현체를 작성함.
- **결과**:
  - `AzureSpeechClient`: SDK 연속 인식(`startContinuousRecognitionAsync`) 패턴 구현 성공. (F-18, F-19)
  - `AzureOpenAIClient`: Spring AI ChatClient 연동. `FAST_MODE` 프롬프트를 통해 1회 호출로 정제/요약 동시 수행. (F-20~F-22)
  - `AzureTranslatorClient`: WebClient 연동 및 `Caffeine` 로컬 캐시 적용. KR->EN->Target 피벗 번역 흐름 구현. (F-23~F-25)
  - `AzureVisionClient`: Vision SDK v1.0.0-beta.1 `analyze` 메서드 연동 및 신뢰도(`minConfidence=0.7`) 기반 필터링 적용. (F-26~F-28)
  - `AzureBlobClient`: `BlockBlobClient` 연동. (F-33, F-34)

### 2.2 이미지 렌더링 엔진
- **내용**: 텍스트 분류, 색상 매핑, 폰트 적용, 이미지 생성(Graphics2D) 구현.
- **결과**:
  - `CategoryClassifier`, `ColorSchemeMapper` 생성 완료.
  - `FontManager` 구현: 프로젝트 내장 폰트(`src/main/resources/fonts/`)를 불러오도록 구현하고 캐싱 적용. (F-30~F-32)
  - `ImageRenderingEngine`: 배경 채우기, 다국어 폰트 매핑 텍스트 렌더링 정상 완료. (F-29)

### 2.3 파이프라인 컨트롤러 및 서비스
- **내용**: `PipelineController` 및 `DefaultPipelineService` 작성.
- **결과**: 오디오, 텍스트, 이미지 3가지 멀티모달 입력에 대응하는 통합 파이프라인 로직 오케스트레이션 적용 완료. (F-03~F-05)

---

## 3. 체크리스트 준수 사항 및 발견된 이슈
- **[점검] Null Check 및 예외 처리**: 외부 클라이언트 API 호출 시 발생하는 예외를 `try-catch`로 래핑하여 에러 상태(`FAILED`)를 반환하도록 안전하게 처리함.
- **[점검] 연결 타임아웃**: WebClient(번역) 및 SDK 생성 시 기본 타임아웃/재시도 룰 적용. (자세한 Resilience4j/Spring Retry 도입은 향후 보완 가능)
- **특이사항**: NLP 모델 응답을 JSON 객체로 파싱하기 위해 시스템 프롬프트에 형식을 강제하였으며, `ObjectMapper`를 사용하여 응답을 `NlpResult`로 직렬화함.

---

## 4. 관련 SAR
- 현재 발견된 누락이나 아키텍처 상 오류(SAR) 없음.
