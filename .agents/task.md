# 📋 AI_Cast 에이전트 작업 할당 보드 (Task Board)

이 문서는 총괄 매니저 **Gale**이 수립한 개발 및 검증 태스크 목록입니다.
개발 에이전트 **Dave**와 **Bake (Baker)**는 자신에게 할당된 태스크를 수행하고 결과를 Gale에게 보고해 주십시오.

---

## 👥 에이전트별 태스크 할당

### 👤 Dave - 사용 모델: DeepSeek V4 Flash
- [x] **T-10. Type A & B 문서 내 깨진 링크 수정 및 정합성 교정**
  - `000_README.md`: 205_RBAC_MENU_GOVERNANCE 링크 제거, .planning/DECISIONS/CONTEXT → PROJECT.md 로 수정, 10_Reference/000_README → 000_Reference_README.md 로 수정
  - `101_ZEN_A4_METHODOLOGY.md`: DECISIONS.md → PROJECT.md, 08_Self_Audit/000_README → 001_Self_Audit_Overview.md 로 수정
  - `102_INTEGRATED_DEVELOPMENT_METHODOLOGY.md`: 08_Self_Audit/000_README → 001_Self_Audit_Overview.md 로 수정 (2건)
  - `206_DOCS_AND_SCRIPT_GOVERNANCE.md`: 205_RBAC_MENU_GOVERNANCE 링크 제거
  - `docs/00_GUIDE/000_README.md`, `docs/00_GUIDE/101_ZEN_A4_METHODOLOGY.md` 등 핵심 설계 가이드 및 규칙 폴더 내의 상대 링크 오류를 전수 검출하여 실존하는 경로로 수정합니다.
  - 예: 존재하지 않는 `205_RBAC_MENU_GOVERNANCE.md` 링크 제거, `.planning/` 및 `10_Reference/` 내의 잘못된 파일명(예: `000_Reference_README.md`) 링크 수정.
- [x] **T-11. Self Audit (SAR) 보고서 공식 작성**
  - `docs/08_Self_Audit/SAR_reports/` 디렉토리에 다음 2종의 누락된 상세 보고서 마크다운 문서를 신설합니다.
    - [NEW] `SAR_2026-04-08_001_Documentation_ReadmeLinkErrors.md` (마크다운 링크 깨짐 이슈 정의 및 조치 내용)
    - [NEW] `SAR_2026-04-17_002_Documentation_DesignConsistency.md` (Zenith LMS 포팅 과정에서의 잔존 명세/명칭 불일치 분석 및 교정 내용)

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [x] **T-12. Type C 문서(템플릿/참조) 내 깨진 링크 수정**
  - `docs/09_TEMPLATES/`, `docs/10_Reference/` 디렉토리 내의 템플릿과 구버전 참조 문서들에 잔존해 있는 깨진 마크다운 링크들을 교정하여 문서 시스템의 무결성을 확보합니다.
- [x] **T-13. 최종 링크 상태 검증 및 Overview 업데이트**
  - 전체 마크다운 파일들의 상대 링크들이 깨짐 없이 100% 정상 참조되는지 유틸리티 또는 스크립트로 최종 검증합니다.
  - 검증 완료 후, [001_Self_Audit_Overview.md](file:///e:/%EB%AA%A8%EB%B9%8C%EB%A6%AC%ED%8B%B0%EC%82%AC%EC%97%85%EB%B3%B8%EB%B6%80/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/2026/vibe%20coding/workspace/AI_Cast/docs/08_Self_Audit/001_Self_Audit_Overview.md) 내의 001 및 002번 건의 상태를 기존 `"미해결"`에서 `"✅ 해결"`로 갱신합니다.
  - 결과: overview 문서 내 날짜 및 002 링크 교차 수정 완료.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
