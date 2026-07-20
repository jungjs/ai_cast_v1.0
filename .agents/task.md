# 📋 AI_Cast 에이전트 작업 할당 보드 (Task Board)

이 문서는 총괄 매니저 **Gale**이 수립한 개발 및 검증 태스크 목록입니다.
개발 에이전트 **Dave**와 **Bake (Baker)**는 자신에게 할당된 태스크를 수행하고 결과를 Gale에게 보고해 주십시오.

---

## 👥 에이전트별 태스크 할당

### 👤 Dave - 사용 모델: DeepSeek V4 Flash
- [x] **T-36. 통계 화면 탭별 날짜 선택 UI(Date Range Picker) 추가 및 스크립트 연동 개발**
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에 `#startDateInput`과 `#endDateInput`, 물결표(`~`) 구분자를 정의합니다.
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트\2026\vibe coding\workspace\AI_Cast\src\main\resources\static\js\stats.js)에서 `daily` 탭일 땐 종료일 선택 영역을 숨기고, `weekly`/`monthly` 탭일 땐 종료일 선택 영역을 보여주는 동적 토글 스크립트를 구현합니다.
  - API 호출 파라미터(`date`, `startDate`, `endDate`)가 탭에 맞춰 올바르게 전달되도록 조회 로직을 보완합니다.

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [x] **T-37. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 검증**
  - 서버를 컴파일 기동 후 통계 화면으로 이동하여 일별/주별/월별 탭 전환에 따른 날짜 UI 렌더링 변경 상태를 점검합니다.
  - 실제 조회를 날려 정상적인 API 연동 및 데이터 필터링을 검증합니다.

### 👤 Gale (Gale) - 사용 모델: Gemini 3.5 Flash (Medium)
- [x] **T-35. 최종 E2E 검증 및 로컬 서버 컴파일 반영**
  - 로컬 서버 컴파일 기동 후 통계 화면에서 실제 DB 지자체 목록이 미려하게 로딩되는지 확인하고, 조회 시 정상 집계 데이터가 노출되는지 확인합니다.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
