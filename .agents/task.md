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
- [x] **T-38. 통계 화면 날짜 상한선(max="오늘") 속성 부여 및 자바스크립트 유효성 검증(Validation Guard) 구현**
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/stats.js) of `initDateInput()` 내부에서 오늘 날짜를 구하고, 각 날짜 입력 요소에 `max` 속성을 지정하는 스크립트를 작성합니다.
  - `loadStats()` 실행 시 사용자가 임의로 입력한 날짜에 대해 오늘 날짜보다 미래이거나 시작일이 종료일보다 클 경우, 경고 토스트(`showToast`)를 띄우고 조회를 차단하는 가드 코드를 탑재합니다.
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에서 스크립트 로드 버전을 `v=3` 으로 변경하여 캐시 버스팅을 갱신합니다.
- [x] **T-40. MonitorController에 대시보드 API 통계 정보 실시간 tb_api_log 직접 쿼리 적용**
  - [MonitorController.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/controller/MonitorController.java)의 `getApiStatus` 메소드 SQL 쿼리를 `v_api_stat` 대신 `tb_api_log` 테이블에 대한 직접 GROUP BY 및 집계로 전환합니다.
- [x] **T-41. StatsService에 오늘 날짜 기준 tb_ai_svc_log 실시간 직접 집계 및 주/월간 병합(Merge) 로직 구현**
  - [StatsService.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/service/log/StatsService.java)의 `getDailyStats()`에 오늘 날짜 조회 분기식을 추가하여 `tb_ai_svc_log`에 대한 직접 Group By 쿼리를 수행합니다.
  - `getWeeklyStats()` 및 `getMonthlyStats()` 내에서 오늘 날짜가 포함된 기간일 시, 통계 테이블(`tb_ai_svc_stat`) 조회 값과 오늘 하루치의 `tb_ai_svc_log` 실시간 집계 값을 취합하여 서비스별로 합산 및 누적 병합(Merge)하여 반환하도록 로직을 작성합니다.

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [x] **T-37. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 검증**
  - 서버를 컴파일 기동 후 통계 화면으로 이동하여 일별/주별/월별 탭 전환에 따른 날짜 UI 렌더링 변경 상태를 점검합니다.
  - 실제 조회를 날려 정상적인 API 연동 및 데이터 필터링을 검증합니다.
- [x] **T-39. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 가드 동작 검증**
  - 변경 내역 컴파일 및 서버 재가동 후 통계화면 달력에서 오늘 이후 날짜 클릭 비활성화 여부를 점검합니다.
  - 미래 날짜 강제 입력 및 시작일 > 종료일 모순 입력 시 경고 토스트 동작 및 조회 차단 로직이 무결한지 브라우저에서 최종 확인합니다.
- [x] **T-42. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 실시간 통계 검증**
  - 변경 내역 컴파일 및 서버 재가동 후 대시보드 및 통계화면의 오늘 날짜 지표가 정상적으로 표출되는지 검증합니다.

### 👤 Gale (Gale) - 사용 모델: Gemini 3.5 Flash (Medium)
- [x] **T-35. 최종 E2E 검증 및 로컬 서버 컴파일 반영**
  - 로컬 서버 컴파일 기동 후 통계 화면에서 실제 DB 지자체 목록이 미려하게 로딩되는지 확인하고, 조회 시 정상 집계 데이터가 노출되는지 확인합니다.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
