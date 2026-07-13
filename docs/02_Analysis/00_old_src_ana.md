# 레거시 파이썬 코드 분석 보고서 (AI Cast Orchestrator)

본 문서는 `docs/10_Reference/old_src/AICast_SmartI`에 위치한 기존 파이썬 기반 AI Cast Orchestrator 시스템의 아키텍처와 주요 로직을 분석한 보고서입니다. 이 분석은 새로운 Java 기반 시스템으로의 마이그레이션 및 기능 구현을 위한 기초 자료로 활용됩니다.

## 1. 프로젝트 개요

- **프로젝트 명**: AI Cast Orchestrator
- **주요 기능**: 지역 방송 음성 데이터를 수집하여 **STT → 표준어 정제 → 요약 → 다국어 번역 → 조건부 이미지 생성 → 클라우드 업로드** 과정을 자동화하는 파이프라인 제공.
- **기술 스택**:
    - **Language**: Python 3.x
    - **Framework**: FastAPI (API 서버)
    - **Cloud Services (Azure)**:
        - Azure Speech SDK (STT)
        - Azure OpenAI (표준어 변환 및 요약)
        - Azure Translator (다국어 번역)
        - Azure Blob Storage (이미지 저장)
    - **Library**: PIL (Pillow, 이미지 생성), pydub (오디오 보정), requests (API 호출).

## 2. 시스템 아키텍처 및 흐름

전체적인 데이터 흐름은 다음과 같습니다.

1.  **Audio In**: 사용자로부터 오디오 파일(wav 등)을 수신.
2.  **STT (Azure Speech)**: 음성을 텍스트로 변환. (필요 시 16kHz/mono/PCM으로 변환 처리)
3.  **Refine & Summarize (Azure OpenAI)**:
    - 사투리나 불분명한 텍스트를 표준어로 정제.
    - 정제된 텍스트를 TV 속보나 공지사항 형식으로 요약.
4.  **Translate (Azure Translator)**: 요약된 내용을 영어(Pivot)를 거쳐 다국어로 번역.
5.  **Render (PIL)**: 번역된 텍스트를 이미지로 변환. 메시지 성격(재난, 공지, 알림)에 따라 배경색 차별화.
6.  **Storage (Azure Blob)**: 생성된 이미지를 Azure Blob Storage에 업로드.

## 3. 모듈별 상세 분석

### 3.1. `main.py` (엔트리포인트 및 오케스트레이션)
- **FastAPI 기반 엔드포인트**:
    - `GET /api/ping`: 서버 상태 확인.
    - `GET /api/latest`: 마지막으로 처리된 결과 조회.
    - `POST /api/process_audio`: 전체 파이프라인 실행. (가장 핵심적인 엔드포인트)
- **오케스트레이션 로직 (`_safe_pipeline`)**:
    - 각 모듈(`ai_cast`, `translator`, `txt2img`, `blob_uploader`)을 순차적으로 호출하여 전체 흐름을 제어.
    - `corr_id`를 생성하여 요청별 추적 로깅 수행.

### 3.2. `source/ai_cast.py` (STT 및 NLP)
- **STT 구현**: Azure Speech SDK의 `start_continuous_recognition`을 사용하여 오디오 파일 전체를 텍스트로 인식.
- **NLP 프롬프트**:
    - `normalize`: "한국 시골 마을 안내방송 멘트를 표준어로 변경"하는 프롬프트 사용.
    - `summarize`: "긴급/공지/알림" 카테고리를 분류하고, 정보를 번호별로 나열하는 TV 속보 형식 요약.
- **FAST_MODE**: 환경 변수 `AI_CAST_FAST_MODE=1` 설정 시, 표준어 변환과 요약을 한 번의 LLM 호출로 처리하여 지연 시간(Latency) 최소화.

### 3.3. `source/translator.py` (다국어 번역)
- **피벗 번역(Pivot Translation)**: 한국어 → 영어 → 타겟 언어 순으로 번역을 수행하여 정확도 향상 시도.
- **캐싱**: 메모리 기반의 `translation_cache`를 사용하여 중복 번역 요청 최소화.
- **기타**: 번역이 불필요한 경우(이미 타겟 언어와 같거나 숫자인 경우)를 필터링하는 `should_skip_translation` 로직 포함.

### 3.4. `source/txt2img.py` (이미지 렌더링)
- **카테고리 자동 감지**: 키워드 매칭(Heuristics)을 통해 메시지를 `disaster(재난)`, `announce(공지)`, `inform(알림)`으로 분류.
- **디자인 설정**:
    - **재난**: 배경색 `COLOR_DISASTER` (예: 옅은 주황계열)
    - **공지**: 배경색 `COLOR_ANNOUNCE` (예: 옅은 파랑계열)
    - **알림**: 배경색 `COLOR_INFORM` (예: 옅은 노랑계열)
- **폰트**: 다국어 지원을 위해 `NotoSans` 가변 폰트 사용.

### 3.5. `source/blob_uploader.py` (클라우드 연동)
- `Azure Storage SDK`를 사용하여 로컬에서 생성된 PNG 파일을 지정된 컨테이너에 업로드하고 접근 가능한 URL을 반환.

## 4. 핵심 로직 및 특징

- **복원력 (Resilience)**: 파이프라인의 각 단계에서 예외 발생 시 로그를 남기고 가능한 범위 내에서 부분 성공 결과를 반환함.
- **로깅 패턴**: `[API][REQ]`, `[API][RES]`, `[API][ERR]` 등 통일된 포맷을 사용하여 운영 시 모니터링이 용이하도록 설계됨.
- **오디오 전처리**: `pydub`를 사용하여 입력 오디오를 STT가 원활히 처리할 수 있는 포맷(16kHz, Mono, PCM)으로 자동 변환.

## 5. Java 마이그레이션 시 고려 사항

1.  **라이브러리 매핑**: 
    - FastAPI -> Spring Boot (MVC or WebFlux)
    - Python `openai` -> Azure OpenAI Java Client
    - PIL/Pillow -> Java `Graphics2D` or `Thumbnailator` (다국어 폰트 렌더링 주의)
    - AudioSegment -> Java `javax.sound.sampled` or `FFmpeg` wrapper
2.  **비동기 처리**: Python의 `asyncio` 및 `ThreadExecutor` 로직을 Spring의 `@Async` 또는 `CompletableFuture`로 전환 필요.
3.  **환경 관리**: `.env` 방식의 설정을 `application.yml` 및 `Profile` 관리 방식으로 전환.

---
**작성일**: 2026-04-19
**분석자**: Antigravity AI Assistant
