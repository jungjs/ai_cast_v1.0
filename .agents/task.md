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
- [x] **T-43. stats.js에 서비스 호출 추이 차트(trendChart) 데이터 업데이트 로직 구현**
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/stats.js)의 `updateCharts(data)` 함수 내에 `trendChart` 데이터셋 가공 및 업데이트 코드를 포팅합니다.
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에서 스크립트 로드 버전을 `v=4` 로 변경하여 캐시 버스팅을 갱신합니다.
- [x] **T-45. MonitorController에 최근 API 호출 로그 10건 반환 엔드포인트(recent-logs) 구현**
  - [MonitorController.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/controller/MonitorController.java)에 `/api/monitor/recent-logs` 엔드포인트를 구현하여 `tb_api_log` 테이블에서 최근 10건의 기록을 역순 정렬 조회 후 반환합니다.
- [x] **T-46. dashboard.js에 API 통계 카드 및 최근 활동 피드 동적 렌더링 구현**
  - [dashboard.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/dashboard.js)에 `fetchApiStatus()`와 `fetchRecentLogs()` 메소드를 추가하고 이를 최초 로딩 및 폴링 루프에 적재합니다.
  - [dashboard.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/dashboard.html)에서 스크립트 임포트 주소 뒤에 캐시 버스팅 파라미터 `(v=2)` 를 매핑합니다.
- [x] **T-48. stats.js에 탭별 동적 날짜 기본값(일/주/월) 리셋 및 가시성 연동 로직 추가**
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/stats.js)의 `toggleDateInputs()`를 수정하여 탭 클릭 시 날짜 기본값(일: 오늘, 주: 이번달 1일, 월: 올해 1월 1일)을 동적으로 채우는 기능을 탑재합니다.
  - `initDateInput()` 내부의 고정 기본값(1개월 전) 세팅 구문을 정비합니다.
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에서 스크립트 로드 버전을 `v=6` 로 설정하여 캐시를 무력화합니다.
- [x] **T-50. StatsResponseDto 생성 및 StatsController 리턴 타입 개조**
  - [StatsResponseDto.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/dto/StatsResponseDto.java) DTO 클래스를 신설하여 `aiStats`, `apiStats`, `trendStats` 리스트 필드를 포함시킵니다.
  - [StatsController.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/controller/StatsController.java)의 `/daily`, `/weekly`, `/monthly` API 응답 포맷을 DTO 객체 형태로 개조합니다.
- [x] **T-51. StatsService 내 API/AI 통계 직접 질의 및 시간/일자 추이 차트 쿼리 병합 구현**
  - [StatsService.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/service/log/StatsService.java)에 `tb_api_log` 기반 엔드포인트별 호출 통계 쿼리 로직을 작성합니다.
  - 일별 조회 시 `HOUR(req_time)` 함수를 사용해 서비스별 시간대(00시~23시) 추이를 질의하고, 주/월별 시 일자별 추이를 연산해 DTO로 포장해 리턴하는 서비스 레이어를 완성합니다.
- [x] **T-52. stats.html 요약 카드 구역 이원화 및 2개 상세 테이블 분리 마크업 개편**
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에 API 통계 카드 구역과 AI 서비스 통계 카드 구역을 분리 배치합니다.
  - 하단 상세 구역에 `#apiStatsTable`과 `#aiStatsTable` 두 개의 데이터 테이블 캔버스를 렌더링하고 버전 쿼리를 `v=7` 로 상향합니다.
- [x] **T-53. stats.js 2개 DataTables 인스턴스 바인딩 및 가변 추이 차트 렌더링 스크립트 작성**
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/stats.js)에서 두 개 상세 테이블의 데이터 렌더링(`updateTable()`) 및 DataTables 바인딩을 리팩토링합니다.
  - `trendChart` 데이터 주입 시, 일별 조회 시에는 `00시`~`23시` 시간대 목록을, 주/월별 조회 시에는 일자 목록을 X축 라벨로 다이나믹 갱신하는 차트 가변 드로잉 로직을 구축합니다.
- [x] **T-55. stats.html 마크업 구조 세로 정렬(API 통계 세트, AI 서비스 통계 세트, 분석 차트 그룹) 개편**
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)의 카드와 테이블의 배치 순서를 바꾸어, API 영역(요약+상세 테이블)이 위에 오고 AI 영역(요약+상세 테이블)이 그 아래에 오며, 차트 분석 그룹들이 최하단으로 정돈되도록 개편합니다.
  - 스크립트 로드 버전을 `v=8` 로 매핑합니다.
- [x] **T-56. stats.css 세로 정렬 마진 및 레이아웃 최적화**
  - [stats.css](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/css/stats.css)의 좌우 2열 컬럼 배치 규칙을 제거하고, 세로형 100% 가득 찬 레이아웃 정렬 스타일을 보정합니다.
- [x] **T-58. stats.html 마크업 구조 좌우 수직 2분할(API 영역, AI 영역) 개편**
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)을 수정하여, API 통계 세트(카드+테이블)가 왼쪽 열에 배치되고 AI 서비스 통계 세트(카드+테이블)가 오른쪽 열에 배치되도록 수직 2분할 컨테이너를 구축합니다.
  - 스크립트 버전을 `v=9` 로 매핑합니다.
- [x] **T-59. stats.css 좌우 2열 컬럼 배치 및 미니 카드 요약 그리드 스타일 추가**
  - [stats.css](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/css/stats.css)에 `.stats-split-layout`, `.stats-column`, `.summary-cards.mini-cards` 스타일 코드를 포팅합니다.
- [x] **T-61. .github/workflows/azure-deploy.yml CI/CD 자동화 파일 신설**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 신규 경로에 Maven 빌드와 Azure App Service 배포를 일련으로 수행하는 YAML 코드를 작성합니다.
- [x] **T-63. GitHub Actions 내 Azure App Name 불일치 오류 픽스**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 파일의 `app-name` 설정을 실물 리소스 명칭인 `aicast-smarti-dev-new` 로 올바르게 교정합니다.
- [x] **T-64. GitHub Actions 플러그인 메이저 버전 최신 격상 (Node 24 호환)**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 내의 checkout, setup-java, upload/download-artifact, webapps-deploy 등의 패키지 버전을 최신으로 업그레이드합니다.
- [x] **T-65. 구형 Container Apps 배포용 deploy.yml 파일 삭제 정리**
  - [.github/workflows/deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/deploy.yml) 파일을 삭제하여 불필요한 이중 트리거 및 버전 Deprecated 장애를 제거합니다.
- [x] **T-66. OneDeploy 400 에러 극복을 위한 단일 JAR 파일 대상 강제 한정 패치**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 파일 내의 `Upload Artifact` 경로 및 배포 `package` 파라미터를 단일 고정 파일인 `ai_cast-1.0.0-SNAPSHOT.jar` 로 교정합니다.
- [x] **T-67. GitHub Actions 단일 Job 구조 통합 및 artifact 플러그인 전격 소거**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 을 리팩토링하여 단일 `build-and-deploy` 작업으로 합치고, `upload/download-artifact` 액션을 제거하여 Node.js 20 Deprecated 충돌을 영구 박멸합니다.
- [x] **T-68. 사용자 수정 @v5 버전 깃허브 워크플로우 최종 원격 동기화**
  - 사용자가 업그레이드 수정한 `azure-deploy.yml` (@v5 버전 격상) 파일을 git commit & push 처리하여 깃허브 액션 배포를 완성합니다.
- [x] **T-69. 빈 커밋(Empty Commit) Push를 통한 GitHub Actions 수동 재배포 트리거**
  - `git commit --allow-empty` 명령 및 `git push`를 통해 깃허브 러너 배포를 강제로 재점화합니다.
- [x] **T-70. GitHub Actions 플러그인 공식 안정 버전(@v4, @v3) 롤백 고정**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 내의 Actions 버전을 공식 호환 스펙인 `checkout@v4`, `setup-java@v4`, `webapps-deploy@v3` 로 변경하여 400 에러를 제거합니다.
- [x] **T-71. 사용자 수정 v5/v3 복합 명세서 동기화 및 즉시 재배포 트리거**
  - 사용자가 최적 조율한 `azure-deploy.yml` (checkout/setup-java@v5, webapps-deploy@v3) 변경 사항을 push하여 재배포를 가동합니다.
- [x] **T-72. 빈 커밋(Empty Commit) Push를 통한 GitHub Actions 수동 재배포 트리거 (2차)**
  - `git commit --allow-empty` 명령 및 `git push`를 통해 깃허브 Actions 빌드를 다시 한번 강제 트리거합니다.
- [x] **T-73. 신규 웹앱 aicast-av-dev 기반 배포 타겟 매핑 및 기동**
  - [.github/workflows/azure-deploy.yml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/.github/workflows/azure-deploy.yml) 의 `app-name` 설정을 신규 리소스명인 `aicast-av-dev` 로 변경 후 원격 push하여 배포를 기동합니다.
- [x] **T-74. Azure Speech SDK 버전 격상을 통한 OpenSSL 3.0 리눅스 호환 패치**
  - [pom.xml](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/pom.xml) 의 `com.microsoft.cognitiveservices.speech` 버전을 `1.34.0` 에서 `1.38.0` 으로 올려 원격 push하여 배포를 기동합니다.
- [x] **T-75. AI 파이프라인 실패 응답 시 HTTP 500 에러 동기화 및 DB 상태 기록 정합성 패치**
  - [PipelineController.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/controller/PipelineController.java) 내의 각 API 메소드에서 `FAILED` 수신 시 `INTERNAL_SERVER_ERROR`를 반환하도록 고친 뒤 push 배포합니다.
- [x] **T-76. 번역(Translate) 연산 시 토큰 대신 글자 수 컬럼(req_size/res_size)으로 가치 분리 수집 패치**
  - [AiSvcLogAspect.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/common/aop/AiSvcLogAspect.java) 를 수정하여 번역 시 토큰은 null로 적재하고, 글자 수를 크기 지표로 매핑하여 push합니다.
- [x] **T-77. STT / OCR 연산 시 추출 텍스트 글자 수를 totalTokens 에 반영 수집 패치**
  - [AiSvcLogAspect.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/common/aop/AiSvcLogAspect.java) 를 수정하여 STT(String), OCR(OcrResult) 호출 완료 시 획득된 텍스트의 글자 수를 totalTokens 필드에 바인딩하여 push합니다.
- [x] **T-78. STT 오디오 파일 파싱 및 재생 시간(초) 계산 로직 구현**
  - [DefaultPipelineService.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/service/pipeline/DefaultPipelineService.java) 에서 오디오 수신 시 헤더를 분석하여 재생 초(sec)를 계산하고 가공 전달하도록 수정합니다.
- [x] **T-79. AOP 로그 수집기 내 STT/STORAGE 비용 지표 매핑 패치**
  - [AiSvcLogAspect.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/common/aop/AiSvcLogAspect.java) 를 수정하여 STT 재생 초를 req_size로, STORAGE 파일 용량을 res_size로 수집하도록 조율합니다.
- [x] **T-80. 로컬 Maven 빌드 및 컴파일 유닛 테스트 검증**
  - 로컬 환경변수 기반 컴파일 및 테스트를 돌려 오류가 없는지 최종 빌드 검증을 진행합니다.
- [/] **T-81. 로컬 Git 커밋 보존 (원격 Push 배포 보류)**
  - 로컬 Git 변경분을 커밋하되, 원격 push는 진행하지 않고 대기합니다.

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [x] **T-37. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 검증**
- [x] **T-39. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 가드 동작 검증**
- [x] **T-42. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 실시간 통계 검증**
- [x] **T-44. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 호출 추이 차트 검증**
- [x] **T-47. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 대시보드 복구 기능 검증**
- [x] **T-49. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 탭별 날짜 기본값 동작 검증**
- [x] **T-54. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 통계 대개편 기능 검증**
- [x] **T-57. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 세로 배치 통계 확인**
- [x] **T-60. 서버 재컴파일 배포 및 웹브라우저 가동 최종 E2E 수직 2분할 배치 확인**
- [x] **T-62. 깃 허브 동기화 및 깃허브 액션 신규 워크플로우 YAML 최종 점검**
  - 원격 깃 허브 저장소에 push 한 뒤 YAML 설정 파일이 프로젝트의 디렉토리 구조 상 정상 인출되는지 확인합니다.
  - 통계 페이지를 리로드하여 화면 왼쪽(API 통계 카드+테이블)과 오른쪽(AI 서비스 통계 카드+테이블)이 수직 경계 기준 50%의 데칼코마니 형태 좌우 레이아웃으로 출력되는지 확인합니다.
  - 그 아래 영역에 라인 추이 및 3종 차트가 무결하게 동작하는지 검증합니다.

### 👤 Gale (Gale) - 사용 모델: Gemini 3.5 Flash (Medium)
- [x] **T-35. 최종 E2E 검증 및 로컬 서버 컴파일 반영**
  - 로컬 서버 컴파일 기동 후 통계 화면에서 실제 DB 지자체 목록이 미려하게 로딩되는지 확인하고, 조회 시 정상 집계 데이터가 노출되는지 확인합니다.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
