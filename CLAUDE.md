# AI_Cast Project (Java Edition)

> **프로젝트:** AI_Cast (STT/LLM 기반 AI 오케스트레이터)
> **문서번호:** Gov-02
> **작성자:** Antigravity (AI Agent)
> **작성일:** 2026-04-19
> **버전:** v1.0

> [!IMPORTANT]
> 이 문서는 전반적인 작업과 Claude 에이전트를 위한 규정입니다.
> **Antigravity 및 Gemini 에이전트** 전용 지침은 [GEMINI.md](GEMINI.md)를 참조하십시오.

## Project Overview

AI_Cast는 음성 데이터를 텍스트로 변환(STT)하고, 표준어 정규화, 요약, 번역 및 이미지 생성을 수행하는 AI 오케스트레이터 파이프라인입니다.

## Tech Stack

- **Language**: Java 1.8 (or 17+)
- **Framework**: Spring Boot 2.7.x / 3.x
- **Build Tool**: Maven (`mvnw` 포함)
- **APIs**: Azure Speech SDK, OpenAI API, Azure Blob Storage

## Key Conventions

- 커밋 메시지: `<type>: <description>` (feat, fix, refactor, docs, test, chore)
- 브랜치 전략: `main` / `feature/*` / `fix/*`
- 코드 스타일: 불변성 우선, 함수 50줄 이하, 파일 800~1,000줄 이하 (초과 시 분리)
- **핵심 가이드라인**: 신규 지시가 기존 설계와 충돌 시 즉시 실행 금지 및 명확한 재확인 필수

## Important Notes

### 1. 핵심 개발 방법론
- 방법론: GSD + ZEN_A4 (경량화 GSD 하이브리드)
- 테스트 커버리지: 80% 이상 필수
- 자체 검증: Phase 1(Self Check), Phase 2(Self Test) 단계 통과 필수
- 오류 관리: 모든 오류는 SAR(Self Audit Report)에 기록 및 체크리스트 업데이트

### 2. 프로젝트 시작 절차
1. Zenith 프로젝트 구조 차용 및 초기화
2. Java/Spring Boot 베이스 코드 구성
3. STT -> LLM -> Translation 파이프라인 포팅

---
*상세 내용은 docs/00_GUIDE/ 내 관련 문서 참조*
