---
name: Documentation Design Consistency
description: Zenith LMS 포팅 과정에서의 잔존 명세/명칭 불일치 분석 및 교정
category: Documentation
severity: MEDIUM
date: 2026-07-13
author: Antigravity (AI Agent)
---

## 현상 (What)

Zenith LMS 프로젝트 구조를 AI_Cast로 포팅(Porting)하는 과정에서 일부 설계 명세, 파일명, 비즈니스 로직 명칭이 Zenith LMS 기준으로 잔존하는 불일치가 발견되었습니다. 특히 번호 체계(Zenith LMS 번호 잔존)와 명칭 불일치(예: `iron` → `Family`)가 주된 문제입니다.

### 발견된 불일치 항목

| 구분 | 내용 | 영향 |
|------|------|------|
| 명칭 불일치 | 비즈니스 로직 내 `iron` 명칭이 `Family`로 변경되지 않음 | 도메인 용어 혼선 |
| 번호 체계 | 일부 설계 문서에 Zenith LMS 시절의 문서 번호 잔존 | 문서 탐색 오류 |
| WBS 불일치 | WBS 상의 일부 항목 번호가 실제 구현과 상이 | 진척도 추적 오류 |
| 문서 링크 | 08_Self_Audit 디렉토리 README 파일명 변경 반영 누락 | 네비게이션 단절 |

---

## 원인 (Why)

### 직접적 원인
Zenith LMS의 기존 문서와 코드 구조를 AI_Cast 프로젝트로 복사한 후, 내부 참조명과 문서 번호를 AI_Cast 체계로 전량 변경하는 작업이 누락되었습니다.

### 근본 원인
- 포팅(Porting) 후 전수 조사(Full Audit) 없이 일부 문서만 선별적으로 수정
- 도메인 용어 변경(`iron`→`Family`)이 설계 문서까지 일괄 반영되지 않음
- WBS(Work Breakdown Structure) 업데이트 시 문서 번호 정합성 검증 부재

### 기여 요소
- 문서 간 교차 참조(Cross-document Sync) 자동화 도구 부재
- 포팅 완료 기준(Criteria)에 "문서 명칭 일관성 검증" 항목 누락

---

## 조치 (How)

### 적용된 조치

1. **명칭 일관성 교정**
   - `An_02`, `An_04` 등 분석 문서 내 Zenith LMS 잔존 명칭 → AI_Cast 표준 명칭으로 일괄 교체
   - 비즈니스 로직 참조 테이블 내 `iron` → `Family` 명칭 변경 반영

2. **문서 번호 체계 정리**
   - WBS(WBS_01_상세_공정표.md) 문서 번호 및 참조 번호 AI_Cast 체계로 현행화
   - 설계 문서(DS_*) 내 교차 참조 번호 정합성 검증

3. **누락 문서 링크 복원**
   - `08_Self_Audit/000_README.md` → `08_Self_Audit/001_Self_Audit_Overview.md` 경로 수정 (2건)

### 수정 범위
- [x] 해당 파일만 수정
- [x] 유사 파일들도 동일 패턴 적용 (WBS, 분석 문서 일괄 교정)
- [ ] 테스트 코드 추가 (해당 없음)
- [x] 문서 업데이트

---

## 검증 (Verification)

1. 변경된 명칭이 AI_Cast 프로젝트 전체 문서에서 일관되게 사용되는지 키워드 검색으로 확인
2. WBS 번호 체계가 실제 구현 단계(WBS Phase 1~5, 요구사항 F-01~F-36)와 정합하는지 교차 검증
3. 08_Self_Audit 디렉토리 링크가 정상 동작하는지 확인

---

## 예방 (Prevention)

1. **Cross-doc Sync 체크리스트 항목 추가**: `PHASE_1_DESIGN_CHECKLIST`에 "문서 간 명칭 일관성 검증" 항목을 신설하여 향후 재발 방지
2. **포팅 완료 기준 강화**: 타 프로젝트에서 포팅 시 "용어/명칭 일관성 전수 조사"를 완료 조건에 포함
3. **정기 명칭 감사**: 월 1회 프로젝트 전체 문서의 용어 및 번호 체계 정합성 검사 수행
