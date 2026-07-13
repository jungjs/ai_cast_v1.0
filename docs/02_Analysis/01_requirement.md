# 요구사항 정의서 (AI Cast 시스템 - Java 기반)

본 문서는 레거시 파이썬 시스템(`AI Cast Orchestrator`)의 기능을 계승하면서, Java(Spring Boot) 환경으로 전환하여 구축할 새로운 AI Cast 시스템의 요구사항을 정의합니다.

## 1. 개요 및 목표

- **목표**: 지역 방송 음성 데이터를 자동 처리하여 다국어 텍스트 및 이미지를 생성하는 고가용성 Java 서버 구축.
- **핵심 가치**: 저지연(Low Latency) 처리, 확장성 있는 모듈 구조, 통계 및 인증 기반 운영 관리 체계 확보.

## 2. 기술 스택 (Physical Requirements)

- **Language**: Java 17 이상
- **Framework**: Spring Boot 3.x
- **Cloud Infrastructure**: Azure Cloud
- **External APIs**:
    - Azure Speech Services (STT)
    - Azure OpenAI Service (Refinement & Summarization)
    - Azure Translator (Translation)
    - Azure Blob Storage (Image Hosting)
- **Library**:
    - Spring Web / Spring Data JPA
    - Azure SDK for Java
    - Java Graphics2D (이미지 렌더링용)
    - Project Lombok

## 3. 기능 요구사항 (Functional Requirements)

### 3.1. API 인터페이스
- **F-01**: 서버 상태 확인 API (`/api/ping`) 제공.
- **F-02**: 마지막 처리 결과 조회 API (`/api/latest`) 제공.
- **F-03**: 음성 처리 파이프라인 API (`/api/process_audio`) 제공. (Multipart 오디오(wav확장자) 및 타겟 언어 코드 수신)
    - 처리순서는 다음과 같다.
    - 음성 데이터(wav) -> text 변환(STT) -> 자연어처리(NLP, 정제와 요약을 단일 프로세스에서 수행) -> 다국어번역(Translation) -> 이미지 생성(Image Rendering)
- **F-04**: 텍스트파일 처리 파이프라인 API (`/api/process_text`) 제공. (Multipart text파일(txt확장자) 및 타겟 언어 코드 수신)
    - 처리순서는 다음과 같다.
    - text 데이터(txt) -> 자연어처리(NLP, 정제와 요약을 단일 프로세스에서 수행) -> 다국어번역(Translation) -> 이미지 생성(Image Rendering)
- **F-05**: 이미지파일 처리 파이프라인 API (`/api/process_img`) 제공. (Multipart 이미지파일(jpg확장자) 및 타겟 언어 코드 수신)
    - 처리순서는 다음과 같다.
    - 이미지 데이터(img) -> image to text 변환(OCR) -> 자연어처리(NLP, 정제와 요약을 단일 프로세스에서 수행) -> 다국어번역(Translation) -> 이미지 생성(Image Rendering) 

### 3.2. API 인증
- **F-06**: 모든 API호출은 api_key로 인증한다. 
- **F-07**: api_key는 기존 DB의 "gov_list" 테이블의 "api_key" 컬럼을 참조한다. 
- **F-08**: API 호출의 유효성 검사는 호출 시 등록된 api_key정보를 gov_list 테이블에서 조회하여, 해당 key가 api_key컬럼에 존재하며, 해당 row의 is_active 필드가 1일때 유효하다, 그 이외는 유효하지 않다.

### 3.3. API내 azure ai 호출로그 기록 및 통계정보 구축
- **F-09**: API 처리 로직에서 STT, NLP, TRANSLATE, IMAGE_GENERATION, OCR 호출 로그를 DB에 저장한다. 
- **F-10**: 로그 저장 시, api_key를 기반으로 저장하여, 누가, 언제, 어떻게 사용했는지를 확인할 수 있도록 한다.
- **F-11**: azure ai 호출통계는 익일 00시 10분에 전 일자 로그를 집계하여 저장한다.
    - 주별, 월별 통게정보는 일별 통계정보를 조회하여 실시간 활용한다. 

### 3.4. azure ai 호출통계 조회 웹페이지 구축
- **F-12**: 3.3에서 구축된 azure ai 호출통계정보를 기반으로 일별, 주별, 월별 호출 통계정보를 조회하는 웹페이지를 구축한다. 
- **F-13**: 기본적으로 해당 페이지는 관리자 전용 페이지이며, 일반사용자는 자신의 api_key정보를 전달하여, 자신의 호출통계정보를 조회할 수 있다.

### 3.5. 시스템 모니터링 정보 구축 및 대시보드 구축
- **F-14**: 본 시스템이 동작하고 있는 azure 컨테이너의 리소스(CPU, 메모리, 네트워크, I/O) 모니터링 정보를 저장해야 하고, 이를 대시보드에 표출해야 한다. 
- **F-15**: 위 리소스 모니터링 정보는 1시간 데이터만 저장하며, 수집주기는 5초로 한다.
- **F-16**: 리소스 모니터링은 모니터링 정보의 실시간 변화 추이를 그래프를 사용하여 표출한다. 
    - 모니터링 표출 시 "80_system_resource_cfg.md"파일을 참조하여 리소스 사용율이 특정 임계치를 초과할 경우, 이를 화면에도 표출하며, 관리자에게 알람을 전달하는 기능을 구현한다. 
    - 알람전달은 슬랙(slack)을 사용한다. 
- **F-17**: 대시보드는 API 실시간 사용 모니터링 정보를 표출해야 한다. 
    - API 사용현황은 "API요청수", "API실패수", "API성공수", "API실패율", "API성공율"을 표시한다. 

### 3.6. 음성 처리 (STT)
- **F-18**: Azure Speech SDK를 이용한 오디오-텍스트 변환.
- **F-19**: 음성 인식 최적화를 위한 오디오 포맷 자동 전처리.

### 3.7. 자연어 처리 (NLP)
- **F-20**: Azure OpenAI를 통한 사투리 표준어 변환.
- **F-21**: 표준화된 텍스트의 방송용 요약 텍스트 생성.
- **F-22**: **FAST_MODE** 지원 (정제와 요약을 단일 프로세스로 수행).

### 3.8. 다국어 번역 (Translation)
- **F-23**: Azure Translator API를 이용한 요약 내용 다국어 번역.
- **F-24**: 정확성 향상을 위한 **피벗 번역(Pivot)** 전략(KR-EN-Target) 적용.
- **F-25**: 동일 텍스트에 대한 번역 결과 캐싱.

### 3.9. Image to Text (OCR)
- **F-26**: Azure Vision AI를 이용한 이미지내 텍스트 추출.
- **F-27**: 입력가능한 이미지 파일을 JPG이며, 크기는 20MB 이내로 한다.
- **F-28**: OCR처리 결과는 텍스트 정제에 사용한다.

### 3.10. 이미지 생성 (Image Rendering)
- **F-29**: 번역된 텍스트의 PNG 이미지 렌더링 구현.
- **F-30**: 키워드 기반 메시지 카테고리(재난/공지/알림) 자동 분류.
- **F-31**: 카테고리에 따른 배경색 차별화 적용.
- **F-32**: 다국어 폰트 지원 및 텍스트 레이아웃 자동 조정.

### 3.11. 클라우드 저장 및 배포 환경
- **F-33**: 생성된 이미지의 Azure Blob Storage 자동 업로드.
- **F-34**: 처리 결과 응답에 이미지 접근 URL 포함.
- **F-35**: CI/CD 환경 구축을 통한 배포 자동화.
- **F-36**: Azure 클라우드 환경 기반의 운영 환경 구성.

## 4. 비기능 요구사항 (Non-Functional Requirements)

### 4.1. 성능 및 가용성
- **NF-01**: 단계별 처리 시간 기록 및 비동기 처리 적용.
- **NF-02**: 대용량 요청 처리를 위한 가용성 확보.

### 4.2. 보안 및 로깅
- **NF-03**: Correlation ID 기반의 전 과정 추적.
- **NF-04**: 시스템 설정 값 및 API Key 보안 관리.

### 4.3. 운영 및 관리
- **NF-05**: 장애 발생 시 관리자 알림 및 정밀 에러 로그 기록.

## 5. 기존 시스템과의 차이점 (Migration Points)

- **언어 및 아키텍처**: Python 기반에서 Java/Spring Boot 기반의 정규 아키텍처로 전환.
- **인증 시스템 도입**: 무인증 방식에서 API 호출 키 기반의 인증 및 식별 시스템으로 강화.
- **운영 중심 확장**: 통계 대시보드 및 자동 배포 시스템을 포함한 운영 관리 기능 확보.

---
**최종 업데이트**: 2026-04-19 (인증 요구사항 반영 및 전체 재색인 완료)
**승인 여부**: 담당자 확인 대기 중
