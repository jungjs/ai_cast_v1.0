# GEMINI.md - AI_Cast Work Regulations

> **프로젝트:** AI_Cast (STT/LLM 기반 AI 오케스트레이터)
> **문서번호:** Gov-01
> **작성자:** Antigravity (AI Agent)
> **작성일:** 2026-04-19
> **버전:** v1.0

이 문서는 AI_Cast 개발에 참여하는 AI 에이전트(Antigravity, Gemini)의 업무 규정을 정의합니다. 모든 에이전트는 이 문서와 `CLAUDE.md`에 명시된 ZEN_A4 방법론을 철저히 준수해야 합니다.

## 1. 업무 태도 및 확인 (Conflict Resolution)
- 사용자 지시와 기존 코드/시스템 설계가 충돌할 경우, 독단적으로 진행하지 않고 반드시 **사용자에게 명확한 확인**을 구하는 절차를 거칩니다.

## 2. 개발 방법론: ZEN_A4 (GSD Hybrid)
에이전트의 모든 작업 수행 시 다음 4단계를 반드시 따릅니다.
- **Phase 1 (Design)**: 설계 및 `Self Check` (필수)
- **Phase 2 (Implement)**: 구현 및 `Self Test` (필수)
- **Phase 3 (Verify)**: 결과 검증 및 교차 체크 (필수)
- **Phase 4 (Commit)**: 규정에 맞는 최종 커밋

## 3. 코드 가이드라인 (ZEN_A4 Core)
- **불변성(Immutability)**: 데이터 구조는 가급적 불변 상태로 설계합니다.
- **길이 제한**: 함수/메소드는 50줄 이하, 개별 파일은 **800 ~ 1,000줄** 이하 유지를 원칙으로 합니다.
- **파일 분리**: 1,000줄 초과 시 기능별 상세 파일로 분리 관리합니다.
- **명칭 준수**: 프로젝트 도메인 용어(STT, Pipeline, Normalization 등)를 정확히 사용합니다.

## 4. 오류 보고 및 학습 (SAR)
작업 중 오류 발생 시 다음 절차를 수행합니다.
1. **SAR 작성**: `docs/08_Self_Audit/` 경로에 `SAR_YYYY-MM-DD_NNN_문제명.md` 형식으로 보고서 작성.
2. **체크리스트 업데이트**: 유사 오류 방지를 위해 관련 Phase 체크리스트에 항목 추가.

## 5. 도구 사용 규칙
- **Java Stack**: Java 8/17 및 Spring Boot 환경에 최적화된 코드를 생성합니다.
- **Build**: Maven을 기본 빌드 도구로 사용하며 `mvnw`를 통해 환경 일관성을 유지합니다.

---

## 개정 이력 (Revision History)

| 버전 | 날짜 | 작성자 | 설명 |
|:---|:---|:---|:---|
| v1.0 | 2026-04-19 | Antigravity | AI_Cast 프로젝트 업무 규정 수립 (ZEN_A4 적용) |
