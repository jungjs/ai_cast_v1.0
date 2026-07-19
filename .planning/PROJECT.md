# Project: AI_Cast (STT/LLM Pipeline)

## 1. 개요
AI_Cast는 다양한 언어의 음성 데이터를 수집하여 텍스트로 변환하고, 이를 정규화 및 요약한 뒤 다국어 번역과 이미지 생성을 수행하는 자동화 파이프라인 시스템입니다.

## 2. 주요 기능 (Porting from Python)
1. **Audio Receiver**: 음성 파일 업로드 및 임시 저장 처리
2. **STT (Speech-To-Text)**: Azure Speech 서비스를 통한 텍스트 추출
3. **Normalization**: 추출된 텍스트를 표준어로 정규화 (LLM 활용)
4. **Summary**: 주요 내용 요약 기술 (LLM 활용)
5. **Translation**: 다국어 번역 (영어, 일본어, 중국어, 베트남어, 러시아어 등)
6. **Image Generation**: 요약된 내용을 기반으로 텍스트 오버레이 이미지 생성
7. **Storage**: 처리 결과 및 이미지를 Azure Blob Storage에 저장

## 3. 기술 스택 (Java Edition)
- **Frontend**: (TBD) Next.js 고려 가능
- **Backend**: Java 1.8+, Spring Boot 2.7+
- **Database**: PostgreSQL (Entity 관리를 위한 JPA 연동 예정)
- **External Services**:
  - OpenAI API (GPT-4o or latest)
  - Azure Cognitive Services (Speech-to-Text)
  - Azure Storage (Blob Containers)
- **CI/CD**: Docker Compose 기반 로컬/서버 배포

## 4. 운영 및 개발 원칙
- **ZEN_A4 방법론 준수**: 모든 작업은 설계 -> 구현 -> 검증 단계를 거치며 에이전트 간 교차 체크 수행
- **파일 구조**: ZENITH.KR.LMS의 프로젝트 구조를 차용하여 체계적인 문서화 및 계획 관리
  - `.planning/`: 프로젝트 로드맵 및 핵심 결정 사항 기록
  - `docs/`: 기술 가이드 및 설계도
  - `messages/`: 다국어 메시지 리소스

## 5. 향후 고도화 및 개선 계획 (Backlog)
추후 시스템 고도화 및 안정성 확보를 위해 순차적으로 추진 가능한 백로그 작업 목록입니다.

1. **대시보드 실시간 금일 사용량 요약 카드 추가 (UI 고도화)**
   * **목표**: 메인 대시보드 화면(`dashboard.html`) 상단 요약 카드에 "금일 누적 AI 토큰/사용량"을 실시간 표출.
   * **대상**: `dashboard.html`, `dashboard.js`, 모니터링 API.
2. **AI 토큰 과금 방지 임계치 실시간 경보 추가 (백엔드 안전장치)**
   * **목표**: 하루/한 달 단위 AI 토큰 소비 임계치 초과 시 `SlackAlertService`를 경유한 긴급 경보 전송.
   * **대상**: `StatsService`, `SlackAlertService`, 스케줄러/로그 로직.
3. **번역 캐시(Caffeine Cache) 비용 절감 효과 시각화 (비용 모니터링)**
   * **목표**: 캐시를 통해 절감한 누적 번역 글자 수 및 추정 절감 금액을 통계 차트화하여 가시성 확보.
   * **대상**: `AzureTranslatorClient`, `StatsService`, `stats.html`/`stats.js`.
4. **testdata-generator 연계 대량 파이프라인 부하 테스트 (안정성 검증)**
   * **목표**: 생성된 다량의 가상 방송 데이터(.wav 오디오 및 .png 카드 이미지)를 파이프라인에 대량 업로드하여 스레드 처리량 및 DB 적재 신뢰성 검증.
   * **대상**: `testdata-generator`, `PipelineController` E2E 통합 테스트.

---
*최종 업데이트: 2026-07-19*

