# AI_Cast 프로젝트 상세 공정표(WBS)

> **프로젝트:** AI Cast 시스템 (Java/Spring Boot 기반)
> **문서번호:** WBS-01
> **작성일자:** 2026-05-12
> **참조문서:** `01_requirement.md`, `02_functionList.md`, `03_Design/*`, `05_UI_UX/*`

---

## 1. WBS 관리 규정 (Governance)

### 상태 코드 (Status Codes)
- **`[ ]` (대기)**: 작업 시작 전
- **`[/]` (진행)**: 현재 작업이 진행 중인 공정
- **`[x]` (완료)**: 작업이 완료되어 검증까지 마친 공정
- **`[!]` (재작업)**: 품질 검수 실패 또는 요건 변경으로 인한 재수정

---

## 2. 전체 프로젝트 진척도 요약

| Phase | 목표 | 예상(MD) | 상태 |
|:---:|:---|:---:|:---:|
| **Phase 1** | 백엔드 인프라 · 인증 · DB 구축 | 4 MD | 대기 |
| **Phase 2** | Azure AI 클라이언트 및 파이프라인 개발 | 10 MD | 완료 |
| **Phase 3** | 로깅 · 통계 · 모니터링 백엔드 | 5 MD | 완료 |
| **Phase 4** | 프론트엔드 대시보드 및 통계 UI | 7 MD | 완료 |
| **Phase 5** | 테스트 · 안정화 · 배포 | 5 MD | 완료 |
| **Total** | | **31 MD** | **완료** |

---

## 3. 상세 공정표 (L1~L4)

### Phase 1: 백엔드 인프라 · 인증 · DB 구축 (4 MD)
> **목표**: Spring Boot 프로젝트 초기화, DB 테이블 생성, API 인증 Filter 구현

#### 1.1 프로젝트 초기 구성 (1 MD)
- **1.1.1 Spring Boot 프로젝트 세팅** (1 MD)
  - [x] 1.1.1.1 Spring Boot 3.x (Java 17+) 프로젝트 생성, 의존성 추가 | 비고: Spring Web, JPA, Swagger, Azure SDK, Lombok
  - [x] 1.1.1.2 `application.yml` 기본 설정 (DB 접속, Azure 설정, 모니터링 설정) | 비고: NF-04

#### 1.2 데이터베이스 구축 (1.5 MD)
- **1.2.1 테이블 생성** (1 MD) | 참조: DS_01_DB_Scheme.md
  - [x] 1.2.1.1 `tb_api_log` (API 호출 로그) 테이블 생성 | 비고: NF-01, NF-03
  - [x] 1.2.1.2 `tb_ai_svc_log` (AI 서비스 호출 로그) 테이블 생성 | 비고: F-09, F-10
  - [x] 1.2.1.3 `tb_ai_svc_stat` (AI 서비스 일별 통계) 테이블 생성 | 비고: F-11
  - [x] 1.2.1.4 `tb_res_log` (컨테이너 리소스 로그) 테이블 생성 | 비고: F-14~F-15
- **1.2.2 인덱스 · 뷰 생성** (0.5 MD)
  - [x] 1.2.2.1 각 테이블 인덱스 생성 (DS_01 §6 참조) | 비고: 성능 최적화
  - [x] 1.2.2.2 `v_api_stat` 통계 조회 뷰 생성 | 비고: F-17

#### 1.3 인증 모듈 구현 (1.5 MD)
- **1.3.1 Filter 기반 인증** (1 MD) | 참조: DS_04_Class_Design.md
  - [x] 1.3.1.1 `CorrelationIdFilter` 구현 (Correlation ID 생성, MDC 전파) | 비고: NF-03
  - [x] 1.3.1.2 `ApiKeyAuthFilter` 구현 (X-API-KEY 헤더 검증, gov_list 조회) | 비고: F-06~F-08
- **1.3.2 인증 서비스** (0.5 MD)
  - [x] 1.3.2.1 `ApiKeyService` 구현 (키 유효성 검사, 호출자 식별, 관리자 판별) | 비고: F-06~F-08, F-13
  - [x] 1.3.2.2 `GovListRepository` 구현 (gov_list 참조 전용 조회) | 비고: 기존 테이블 READ

---

### Phase 2: Azure AI 클라이언트 및 파이프라인 개발 (10 MD)
> **목표**: Azure AI 서비스 연동 클라이언트 구현, 멀티소스 파이프라인 오케스트레이션

#### 2.1 Azure AI 클라이언트 구현 (5 MD)
- **2.1.1 STT 클라이언트** (1 MD) | 참조: 81_AZURE_STT_Method.md
  - [x] 2.1.1.1 `SttClient` 인터페이스 정의 | 비고: F-18
  - [x] 2.1.1.2 `AzureSpeechClient` 구현 (연속 인식, 오디오 전처리) | 비고: F-18, F-19
- **2.1.2 NLP 클라이언트** (1 MD) | 참조: 82_AZURE_NLP_Method.md
  - [x] 2.1.2.1 `NlpClient` 인터페이스 정의 | 비고: F-20
  - [x] 2.1.2.2 `AzureOpenAIClient` 구현 (사투리 정제 + 요약, FAST_MODE) | 비고: F-20~F-22
- **2.1.3 번역 클라이언트** (1 MD) | 참조: 83_AZURE_Translate_Method.md
  - [x] 2.1.3.1 `TranslateClient` 인터페이스 정의 | 비고: F-23
  - [x] 2.1.3.2 `AzureTranslatorClient` 구현 (피벗 번역, 캐싱) | 비고: F-23~F-25
- **2.1.4 OCR 클라이언트** (1 MD) | 참조: 84_AZURE_OCR_Method.md
  - [x] 2.1.4.1 `OcrClient` 인터페이스 정의 | 비고: F-26
  - [x] 2.1.4.2 `AzureVisionClient` 구현 (Read OCR, 20MB 제한) | 비고: F-26~F-28
- **2.1.5 Storage 클라이언트** (1 MD)
  - [x] 2.1.5.1 `BlobClient` 인터페이스 정의 | 비고: F-33
  - [x] 2.1.5.2 `AzureBlobClient` 구현 (PNG 업로드, URL 반환) | 비고: F-33, F-34

#### 2.2 이미지 렌더링 엔진 (2 MD)
- **2.2.1 Engine Layer 구현** (2 MD)
  - [x] 2.2.1.1 `CategoryClassifier` 구현 (키워드 기반 재난/공지/알림 분류) | 비고: F-30
  - [x] 2.2.1.2 `ColorSchemeMapper` 구현 (카테고리별 배경색 매핑) | 비고: F-31
  - [x] 2.2.1.3 `FontManager` 구현 (NotoSans 다국어 폰트, 레이아웃 조정) | 비고: F-32
  - [x] 2.2.1.4 `ImageRenderingEngine` 구현 (Graphics2D PNG 생성) | 비고: F-29

#### 2.3 파이프라인 오케스트레이션 (3 MD)
- **2.3.1 파이프라인 서비스** (2 MD)
  - [x] 2.3.1.1 `PipelineService` 인터페이스 정의 | 비고: F-03~F-05
  - [x] 2.3.1.2 `DefaultPipelineService` 구현 (Audio: STT→NLP→번역→이미지→업로드) | 비고: F-03
  - [x] 2.3.1.3 Text 파이프라인 분기 (NLP→번역→이미지→업로드, STT 생략) | 비고: F-04
  - [x] 2.3.1.4 Image 파이프라인 분기 (OCR→NLP→번역→이미지→업로드) | 비고: F-05
- **2.3.2 파이프라인 컨트롤러** (1 MD) | 참조: DS_02_API_List.md
  - [x] 2.3.2.1 `PipelineController` 구현 (API-01~05: ping, latest, process_audio/text/img) | 비고: Multipart/form-data
  - [x] 2.3.2.2 요청/응답 DTO 정의 | 비고: DS_03_API_Specification.md §2

---

### Phase 3: 로깅 · 통계 · 모니터링 백엔드 (5 MD)
> **목표**: AI 호출 로그 자동 기록, 통계 집계, 컨테이너 리소스 모니터링, 장애 알림

#### 3.1 로깅 시스템 구축 (2 MD)
- **3.1.1 API 호출 로그** (0.5 MD) | 참조: 85_logging_method.md
  - [x] 3.1.1.1 `ApiLogService` 구현 (tb_api_log 비동기 저장) | 비고: NF-01
  - [x] 3.1.1.2 `TbApiLog` 엔티티 + `TbApiLogRepository` 구현 | 비고: DS_01
- **3.1.2 AI 서비스 호출 로그** (1 MD)
  - [x] 3.1.2.1 `AiSvcLogAspect` AOP 구현 (@Around, 자동 로깅) | 비고: F-09, F-10
  - [x] 3.1.2.2 `AiSvcLogService` 구현 (tb_ai_svc_log 비동기 저장) | 비고: F-09, F-10
  - [x] 3.1.2.3 `TbAiSvcLog` 엔티티 + `TbAiSvcLogRepository` 구현 | 비고: DS_01
- **3.1.3 전역 예외 처리** (0.5 MD)
  - [x] 3.1.3.1 `GlobalExceptionHandler` 구현 (@RestControllerAdvice) | 비고: NF-05
  - [x] 3.1.3.2 `ApiKeyMasker` 유틸 구현 (로그 내 민감정보 마스킹) | 비고: NF-04

#### 3.2 통계 집계 시스템 (1 MD)
- **3.2.1 통계 서비스 및 스케줄러** (1 MD)
  - [x] 3.2.1.1 `StatsService` 구현 (일별 집계 쿼리, @Scheduled 00:10) | 비고: F-11
  - [x] 3.2.1.2 `TbAiSvcStat` 엔티티 + `TbAiSvcStatRepository` 구현 | 비고: DS_01
  - [x] 3.2.1.3 주별/월별 통계 조회 메서드 (일별 합산 쿼리) | 비고: F-12

#### 3.3 리소스 모니터링 (1.5 MD)
- **3.3.1 리소스 수집 서비스** (1 MD) | 참조: 80_system_resource_cfg.md
  - [x] 3.3.1.1 `ResourceMonitorService` 구현 (@Scheduled 5초, 1시간 보존) | 비고: F-14, F-15
  - [x] 3.3.1.2 `TbResLog` 엔티티 + `TbResLogRepository` 구현 | 비고: DS_01
  - [x] 3.3.1.3 1시간 초과 데이터 자동 삭제 스케줄러 | 비고: F-15
- **3.3.2 알림 서비스** (0.5 MD)
  - [x] 3.3.2.1 `SlackAlertService` 구현 (Webhook, 임계치별 알림) | 비고: NF-05, F-16
  - [x] 3.3.2.2 디바운싱 로직 (Critical: 60초, Emergency: 즉시) | 비고: F-16

#### 3.4 통계 · 모니터링 컨트롤러 (0.5 MD)
- **3.4.1 API 엔드포인트** (0.5 MD) | 참조: DS_02_API_List.md
  - [x] 3.4.1.1 `StatsController` 구현 (API-06~08: daily/weekly/monthly) | 비고: F-12, F-13
  - [x] 3.4.1.2 `MonitorController` 구현 (API-09: resources, API-10: api-status) | 비고: F-14~F-17

---

### Phase 4: 프론트엔드 대시보드 및 통계 UI (7 MD)
> **목표**: Thymeleaf + Chart.js 기반 대시보드(WEB-01) 및 통계 페이지(WEB-02) 구현

#### 4.1 공통 UI 기반 구축 (1 MD)
- **4.1.1 Thymeleaf 공통 레이아웃** (1 MD)
  - [x] 4.1.1.1 공통 레이아웃 템플릿 (헤더, Dark Mode, Inter 폰트) | 비고: Bootstrap 5
  - [x] 4.1.1.2 공통 CSS 변수 정의 (Glassmorphism, 색상 체계) | 비고: UI_04, UI_05 참조
  - [x] 4.1.1.3 API Key 입력 모달 (인증 공통 컴포넌트) | 비고: F-06

#### 4.2 WEB-01: 대시보드 구현 (3 MD)
- **4.2.1 대시보드 페이지** (3 MD) | 참조: UI_04_WEB01_UISpecification.md
  - [x] 4.2.1.1 `dashboard.html` 레이아웃 구현 (카드 5개 + 차트 2×2 + 로그) | 비고: F-14~F-17
  - [x] 4.2.1.2 `dashboard.js` — API-09 폴링(5초) 및 리소스 차트 렌더링 | 비고: Chart.js + streaming
  - [x] 4.2.1.3 `dashboard.js` — API-10 폴링(10초) 및 카드/활동로그 갱신 | 비고: F-17
  - [x] 4.2.1.4 임계치 Alert Banner 표출 로직 (4단계 위험도) | 비고: F-16
  - [x] 4.2.1.5 `dashboard.css` Dark Mode + Glassmorphism + 반응형 | 비고: UI_04 §5

#### 4.3 WEB-02: AI 호출 통계 구현 (3 MD)
- **4.3.1 통계 페이지** (3 MD) | 참조: UI_05_WEB02_UISpecification.md
  - [x] 4.3.1.1 `stats.html` 레이아웃 구현 (탭 + 카드 4개 + 차트 4종 + 테이블) | 비고: F-12
  - [x] 4.3.1.2 `stats.js` — 탭 전환(일/주/월) 및 API-06~08 비동기 호출 | 비고: F-12
  - [x] 4.3.1.3 `stats.js` — Multi-Line, Doughnut, Bar 차트 렌더링 | 비고: Chart.js
  - [x] 4.3.1.4 `stats.js` — DataTable (정렬/페이징/CSV 내보내기) 구현 | 비고: DataTables.js
  - [x] 4.3.1.5 관리자/일반 권한별 UI 분기 (지자체 필터, 컬럼 표시) | 비고: F-13
  - [x] 4.3.1.6 `stats.css` 통계 페이지 스타일링 | 비고: UI_05 §5

---

### Phase 5: 테스트 · 안정화 · 배포 (5 MD)
> **목표**: 단위/통합/E2E 테스트 수행, CI/CD 구축, Azure 운영 환경 배포

#### 5.1 단위 · 통합 테스트 (2 MD)
- **5.1.1 백엔드 테스트** (2 MD)
  - [x] 5.1.1.1 파이프라인 서비스 유닛 테스트 (JUnit, Mockito) | 비고: F-03~F-05
  - [x] 5.1.1.2 Azure AI 클라이언트 모의 테스트 (MockWebServer) | 비고: F-18~F-28
  - [x] 5.1.1.3 이미지 렌더링 엔진 통합 테스트 (다국어 폰트 검증) | 비고: F-29~F-32
  - [x] 5.1.1.4 통계 집계 서비스 테스트 (일/주/월 집계 검증) | 비고: F-11~F-12

#### 5.2 E2E 연동 테스트 (1 MD)
- **5.2.1 프론트-백 연동** (1 MD)
  - [x] 5.2.1.1 대시보드 실시간 폴링 연동 테스트 (리소스 + API 현황) | 비고: WEB-01
  - [x] 5.2.1.2 통계 페이지 필터/탭 전환 연동 테스트 | 비고: WEB-02
  - [x] 5.2.1.3 관리자/일반 사용자 권한별 시나리오 검증 | 비고: F-13

#### 5.3 배포 파이프라인 (2 MD)
- **5.3.1 CI/CD 구성** (1 MD)
  - [x] 5.3.1.1 Azure DevOps / GitHub Actions 배포 파이프라인 작성 | 비고: F-35
  - [x] 5.3.1.2 Docker 컨테이너 이미지 빌드 스크립트 작성 | 비고: F-36
- **5.3.2 운영 환경 배포** (1 MD)
  - [x] 5.3.2.1 Azure Container Apps 배포 및 환경 변수 설정 | 비고: F-36
  - [x] 5.3.2.2 Health Check(API-01) 확인 및 최종 점검 | 비고: F-01

---

## 4. 요구사항 트레이서빌리티 (Traceability)

| 요구사항 | WBS 항목 | Phase |
|:---:|:---|:---:|
| F-01~F-02 | 2.3.2 파이프라인 컨트롤러 | P2 |
| F-03~F-05 | 2.3.1 파이프라인 서비스 | P2 |
| F-06~F-08 | 1.3 인증 모듈 | P1 |
| F-09~F-10 | 3.1.2 AI 서비스 호출 로그 | P3 |
| F-11 | 3.2.1 통계 서비스 | P3 |
| F-12~F-13 | 3.4.1 통계 컨트롤러 + 4.3 WEB-02 | P3+P4 |
| F-14~F-17 | 3.3 리소스 모니터링 + 4.2 WEB-01 | P3+P4 |
| F-18~F-19 | 2.1.1 STT 클라이언트 | P2 |
| F-20~F-22 | 2.1.2 NLP 클라이언트 | P2 |
| F-23~F-25 | 2.1.3 번역 클라이언트 | P2 |
| F-26~F-28 | 2.1.4 OCR 클라이언트 | P2 |
| F-29~F-32 | 2.2 이미지 렌더링 엔진 | P2 |
| F-33~F-34 | 2.1.5 Storage 클라이언트 | P2 |
| F-35~F-36 | 5.3 배포 파이프라인 | P5 |
| NF-01 | 3.1.1 API 호출 로그 (비동기) | P3 |
| NF-03 | 1.3.1.1 CorrelationIdFilter | P1 |
| NF-04 | 1.1.1.2 + 3.1.3.2 설정/마스킹 | P1+P3 |
| NF-05 | 3.1.3.1 + 3.3.2 예외처리/알림 | P3 |

---
**최종 업데이트**: 2026-05-12
**특이사항**: 기존 3Phase→5Phase 재구성, 요구사항 재번호(F-01~F-36) 전수 반영, DB 테이블명 약어 반영(tb_api_log 등), 인증 Filter 전환, OCR 클라이언트 추가, AI 호출 로그/통계/모니터링 Phase 3 분리, Thymeleaf UI Phase 4 분리, 요구사항 트레이서빌리티 매핑표 추가
