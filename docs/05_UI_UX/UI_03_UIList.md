# UI 목록 (UI List)

본 문서는 AI Cast 시스템에서 제공하는 모든 웹 UI 화면의 목록과 구성 요약을 정리합니다.

---

## 1. UI 화면 목록

| UI ID | 화면명 | URL | 권한 | 요구사항 | 연동 API |
|:---:|:---|:---|:---:|:---:|:---|
| **WEB-01** | 시스템 모니터링 대시보드 | `/dashboard` | 관리자 | F-14~F-17 | API-09, API-10 |
| **WEB-02** | AI 호출 통계 조회 | `/stats` | 필요 (관리자/일반) | F-12, F-13 | API-06~API-08 |

---

## 2. 화면별 상세

### 2.1. WEB-01: 시스템 모니터링 대시보드 (`/dashboard`)

| 항목 | 내용 |
|:---|:---|
| **목적** | 컨테이너 리소스 및 API 사용 현황 실시간 모니터링 |
| **권한** | 관리자 전용 |
| **갱신 주기** | 리소스: 5초, API 현황: 10초 (폴링) |
| **기획 문서** | UI_01_Dashboard_Planning.md |

#### 구성 컴포넌트

| 컴포넌트 | 유형 | 데이터 | 요구사항 |
|:---|:---|:---|:---:|
| API 사용 현황 카드 (5개) | Summary Tiles | 요청수, 성공수, 실패수, 성공율, 실패율 | F-17 |
| CPU 사용률 차트 | Line Chart (실시간) | `tb_res_log.cpu_pct` | F-14, F-16 |
| 메모리 사용량 차트 | Line Chart + 한도선 | `tb_res_log.mem_mb` | F-14, F-16 |
| 네트워크 I/O 차트 | Stacked Area Chart | `tb_res_log.net_rx`, `net_tx` | F-14, F-16 |
| 디스크 I/O 차트 | Stacked Area Chart | `tb_res_log.disk_rd`, `disk_wr` | F-14, F-16 |
| 임계치 알람 배너 | Alert Banner | 위험도 4단계 (정상/주의/경고/위험) | F-16 |
| 최근 활동 로그 | Activity Feed (10건) | `tb_api_log` 최신 | F-17 |

---

### 2.2. WEB-02: AI 호출 통계 조회 (`/stats`)

| 항목 | 내용 |
|:---|:---|
| **목적** | Azure AI 서비스 호출 통계 일별/주별/월별 시각화 |
| **권한** | 관리자: 전체 조회, 일반: 자신의 api_key 통계만 |
| **갱신** | 필터/기간 변경 시 비동기 Fetch |
| **기획 문서** | UI_04_AI_CALL_STAT_Planning.md |

#### 구성 컴포넌트

| 컴포넌트 | 유형 | 데이터 | 요구사항 |
|:---|:---|:---|:---:|
| 기간 선택 탭 | Tab (일별/주별/월별) | API-06~08 전환 | F-12 |
| 요약 카드 (4개) | Summary Tiles | 총 호출수, 성공수, 실패수, 성공률 | F-12 |
| 호출 추이 차트 | Multi-Line Chart | 서비스별 일별 호출 수 | F-12 |
| 서비스별 비율 차트 | Doughnut Chart | `svc_type`별 `tot_cnt` 비율 | F-12 |
| 성공/실패 비율 차트 | Doughnut Chart | `ok_cnt` vs `fail_cnt` | F-12 |
| 평균 처리시간 차트 | Horizontal Bar | 서비스별 `avg_ms` | F-12 |
| 상세 통계 테이블 | DataTable (페이징/정렬/CSV) | `tb_ai_svc_stat` 상세 | F-12, F-13 |
| 서비스/지자체 필터 | Dropdown | `svc_type`, `gov_name` | F-13 |

---

## 3. 공통 디자인 사양

| 항목 | 설정 |
|:---|:---|
| 테마 | **Dark Mode** + **Glassmorphism** |
| 배경 | `#1a1a2e` |
| 카드 배경 | `rgba(255,255,255,0.05)`, `backdrop-filter: blur(10px)` |
| 폰트 | Google Fonts `Inter` |
| 차트 라이브러리 | Chart.js (+ chartjs-plugin-streaming) |
| CSS 프레임워크 | Bootstrap 5 |
| 렌더링 | Spring Boot Thymeleaf (SSR) |
| 인증 | `X-API-KEY` 헤더 기반 |

---

## 4. UI 문서 목록

| 문서 | 파일명 | 내용 |
|:---:|:---|:---|
| UI-01 | UI_01_Dashboard_Planning.md | 시스템 모니터링 대시보드 기획서 (WEB-01) |
| UI-02 | UI_02_Dashboard_Detail_Design.md | 대시보드 상세 화면 설계서 |
| UI-03 | **UI_03_UIList.md** | UI 화면 목록 (본 문서) |
| UI-04 | UI_04_WEB01_UISpecification.md | WEB-01 대시보드 UI 상세설계서 |
| UI-05 | UI_05_WEB02_UISpecification.md | WEB-02 AI 호출 통계 UI 상세설계서 |

---

## 5. 요구사항-UI 매핑표

| 요구사항 | UI ID | 화면 | 컴포넌트 |
|:---:|:---:|:---|:---|
| F-12 | WEB-02 | AI 호출 통계 | 탭, 차트, 테이블 전체 |
| F-13 | WEB-02 | AI 호출 통계 | 권한별 필터, 지자체 드롭다운 |
| F-14 | WEB-01 | 대시보드 | 리소스 수집·표출 (CPU, 메모리, 네트워크, I/O) |
| F-15 | WEB-01 | 대시보드 | 5초 갱신, 1시간 데이터 보존 |
| F-16 | WEB-01 | 대시보드 | 실시간 그래프, 임계치 알람 배너, Slack 알림 |
| F-17 | WEB-01 | 대시보드 | API 현황 카드 (요청수, 성공/실패수, 성공/실패율) |

---
**최종 업데이트**: 2026-05-12
