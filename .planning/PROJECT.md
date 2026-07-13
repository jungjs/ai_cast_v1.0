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

---
*최종 업데이트: 2026-04-19*
