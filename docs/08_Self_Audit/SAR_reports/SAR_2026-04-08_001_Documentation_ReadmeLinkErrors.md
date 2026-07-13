---
name: Documentation Readme Link Errors
description: docs/00_GUIDE/000_README.md 및 핵심 설계 가이드 내 상대 링크 오류 전수 조치
category: Documentation
severity: HIGH
date: 2026-04-08
author: Antigravity (AI Agent)
---

## 현상 (What)

docs/00_GUIDE/000_README.md 및 docs/00_GUIDE/ 내 여러 설계 가이드 문서들에서 존재하지 않는 파일을 참조하는 상대 마크다운 링크가 다수 발견되었습니다. 문서를 따라 이동할 때 404(File Not Found) 상황이 발생하여 문서 탐색 흐름이 단절됩니다.

**발생 위치:** docs/00_GUIDE/000_README.md, docs/00_GUIDE/101_ZEN_A4_METHODOLOGY.md, docs/00_GUIDE/102_INTEGRATED_DEVELOPMENT_METHODOLOGY.md, docs/00_GUIDE/206_DOCS_AND_SCRIPT_GOVERNANCE.md

### 발견된 깨진 링크 목록

| 소스 파일 | 대상 경로 | 문제 |
|-----------|----------|------|
| `000_README.md` | `./205_RBAC_MENU_GOVERNANCE.md` | 파일 미존재 (작성 예정) |
| `000_README.md` | `../../.planning/DECISIONS.md` | 파일 미존재 |
| `000_README.md` | `../../.planning/CONTEXT.md` | 파일 미존재 |
| `000_README.md` | `../10_Reference/000_README.md` | 실제 파일명은 `000_Reference_README.md` |
| `101_ZEN_A4_METHODOLOGY.md` | `../../.planning/DECISIONS.md` | 파일 미존재 |
| `101_ZEN_A4_METHODOLOGY.md` | `../08_Self_Audit/000_README.md` | 실제 파일명은 `001_Self_Audit_Overview.md` |
| `102_INTEGRATED_DEVELOPMENT_METHODOLOGY.md` | `../08_Self_Audit/000_README.md` | 실제 파일명은 `001_Self_Audit_Overview.md` (2건) |
| `206_DOCS_AND_SCRIPT_GOVERNANCE.md` | `./205_RBAC_MENU_GOVERNANCE.md` | 파일 미존재 (작성 예정) |

---

## 원인 (Why)

### 직접적 원인
Zenith LMS 프로젝트 구조를 AI_Cast로 포팅하는 과정에서 일부 참조 문서의 파일명이 변경되었거나 아직 작성되지 않은 상태에서 링크가 먼저 추가되었습니다.

### 근본 원인
- 문서 생성과 링크 추가가 동시에 이루어지지 않아 정합성 불일치 발생
- `.planning/` 디렉토리 구조가 단순화되면서(PROJECT.md만 존재) 기존 DECISIONS.md/CONTEXT.md 참조가 잔존
- `10_Reference/` README 파일명이 규칙과 다르게 `000_Reference_README.md`로 생성되었으나 기존 링크는 `000_README.md`로 유지

### 기여 요소
- 문서 링크 정합성을 자동으로 검증하는 도구 부재
- 문서 생성 후 교차 검증(Cross-doc Sync) 절차 미흡

---

## 조치 (How)

### 수정 내역

1. **000_README.md**
   - `205_RBAC_MENU_GOVERNANCE.md` 링크 → `(추가 예정)` 텍스트로 대체
   - `.planning/DECISIONS.md`, `.planning/CONTEXT.md` → `.planning/PROJECT.md` 로 통합
   - `10_Reference/000_README.md` → `10_Reference/000_Reference_README.md` 로 수정

2. **101_ZEN_A4_METHODOLOGY.md**
   - `.planning/DECISIONS.md` → `.planning/PROJECT.md` 로 수정
   - `08_Self_Audit/000_README.md` → `08_Self_Audit/001_Self_Audit_Overview.md` 로 수정

3. **102_INTEGRATED_DEVELOPMENT_METHODOLOGY.md**
   - `08_Self_Audit/000_README.md` → `08_Self_Audit/001_Self_Audit_Overview.md` 로 수정 (2건)

4. **206_DOCS_AND_SCRIPT_GOVERNANCE.md**
   - 상단 네비게이션의 `205_RBAC_MENU_GOVERNANCE` 링크 → `(추가 예정)` 으로 대체

### 수정 범위
- [x] 해당 링크만 수정
- [ ] 유사 파일들도 동일 패턴 적용 (별도 이슈로 관리)
- [ ] 테스트 코드 추가 (해당 없음)
- [x] 문서 업데이트

---

## 검증 (Verification)

수정 후 각 문서의 링크를 클릭하여 실제 파일이 정상 열리는지 수동 확인 완료. 존재하지 않는 파일을 가리키는 8건의 깨진 링크가 모두 정상 경로 또는 "(추가 예정)" 텍스트로 교정되었습니다.

---

## 예방 (Prevention)

1. 신규 문서 생성 시 템플릿의 예시 링크를 실제 파일 경로로 반드시 변경
2. 문서 링크 추가 후 대상 파일이 존재하는지 `Test-Path` 등으로 확인하는 습관화
3. 정기적인 문서 링크 정합성 검사 수행 (Type C 문서 링크 정리 후 최종 검증 예정)
