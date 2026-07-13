# UI 상세설계서 — WEB-02: AI 호출 통계 조회

**화면 ID**: WEB-02 | **URL**: `/stats` | **권한**: 필요 (관리자/일반)
**요구사항**: F-12, F-13 | **연동 API**: API-06, API-07, API-08

---

## 1. 화면 구조

```
┌──────────────────────────────────────────────────────────┐
│ [A] 헤더: 📊 AI 호출 통계    /dashboard링크   [사용자명]   │
├──────────────────────────────────────────────────────────┤
│ [B] 조회 조건 영역                                        │
│  [일별] [주별] [월별]  📅기간선택  서비스▼  지자체▼(관리자)  │
├──────────────────────────────────────────────────────────┤
│ [C] 요약 카드 (4개 수평)                                  │
│  총 호출수 | 성공수 | 실패수 | 성공률                       │
├──────────────────────────────────────────────────────────┤
│ [D] 호출 추이 차트 (Multi-Line)                           │
│  서비스별(STT/NLP/TRANSLATE/OCR/IMG) 일별 호출 수          │
├──────────────────────────────────────────────────────────┤
│ [E-1] 서비스별 비율     [E-2] 성공/실패 비율                │
│  (Doughnut Chart)      (Doughnut Chart)                  │
├──────────────────────────────────────────────────────────┤
│ [F] 평균 처리시간 (Horizontal Bar)                        │
│  STT ██████ 2340ms | NLP ████ 1520ms | ...              │
├──────────────────────────────────────────────────────────┤
│ [G] 상세 통계 테이블 (DataTable)                          │
│  날짜 | (지자체) | 서비스 | 호출수 | 성공 | 실패 | 평균ms   │
│  페이징 | 정렬 | CSV 내보내기                               │
└──────────────────────────────────────────────────────────┘
```

---

## 2. 컴포넌트 설계

### [A] 헤더

| 요소 | 설명 |
|:---|:---|
| 타이틀 | "📊 AI 호출 통계" |
| 대시보드 링크 | `/dashboard` 이동 (관리자만 표시) |
| 사용자명 | 인증된 `gov_name` 표시 |
| API Key 입력 | 미인증 시 api_key 입력 모달 표출 |

### [B] 조회 조건 영역 (F-12)

#### 탭 전환

| 탭 | API | 날짜 UI |
|:---|:---|:---|
| **일별** | API-06 (`/api/stats/daily`) | DateRange Picker (시작~종료) |
| **주별** | API-07 (`/api/stats/weekly`) | 연도 + 주차 선택 |
| **월별** | API-08 (`/api/stats/monthly`) | 연도 + 월 선택 |

#### 필터

| 필터 | 타입 | 옵션 | 비고 |
|:---|:---|:---|:---|
| 서비스 필터 | Dropdown | 전체, STT, NLP, TRANSLATE, OCR, IMAGE_GEN | 공통 |
| 지자체 필터 | Dropdown | 전체 / 개별 지자체 | 관리자만 표시 (F-13) |

- 탭/필터/기간 변경 → Fetch API 비동기 호출 → 모든 차트+카드+테이블 동시 갱신

### [C] 요약 카드 (4개)

| 카드 | 데이터 | 색상 |
|:---|:---|:---|
| 총 호출수 | `SUM(tot_cnt)` | `#3498db` |
| 성공수 | `SUM(ok_cnt)` | `#2ecc71` |
| 실패수 | `SUM(fail_cnt)` | `#e74c3c` (0이면 Green) |
| 성공률 | `ok_cnt/tot_cnt * 100` | ≥95: Green, ≥80: Yellow, <80: Red |

### [D] 호출 추이 차트

| 속성 | 설정 |
|:---|:---|
| 유형 | Multi-Line Chart |
| X축 | 날짜 (`stat_dt`) |
| Y축 | 호출 수 |
| 시리즈 | STT(`#3498db`), NLP(`#2ecc71`), TRANSLATE(`#e74c3c`), OCR(`#f39c12`), IMAGE_GEN(`#9b59b6`) |
| 인터랙션 | hover 툴팁, 범례 클릭으로 시리즈 토글 |

### [E] 비율 차트 (2개 Doughnut)

| 차트 | 데이터 | 색상 |
|:---|:---|:---|
| E-1 서비스별 비율 | `svc_type`별 `tot_cnt` | 서비스별 고유 색상 |
| E-2 성공/실패 비율 | `ok_cnt` vs `fail_cnt` | Green / Red |

### [F] 평균 처리시간

| 속성 | 설정 |
|:---|:---|
| 유형 | Horizontal Bar Chart |
| 항목 | 서비스별 `avg_ms` (ms) |
| 색상 | 서비스별 고유 색상 |
| 정렬 | 처리시간 내림차순 |

### [G] 상세 통계 테이블

| 컬럼 | 데이터 | 비고 |
|:---|:---|:---|
| 날짜 | `stat_dt` | |
| 지자체 | `gov_name` | 관리자만 표시 (F-13) |
| 서비스 | `svc_type` | 배지 스타일 |
| 총 호출 | `tot_cnt` | 숫자 포맷 (,) |
| 성공 | `ok_cnt` | Green |
| 실패 | `fail_cnt` | Red (0이면 Green) |
| 성공률 | 계산값 (%) | 조건부 색상 |
| 평균 처리시간 | `avg_ms` (ms) | |

- **페이징**: 20건/페이지
- **정렬**: 컬럼 헤더 클릭 (ASC/DESC 토글)
- **CSV**: "CSV 다운로드" 버튼 (현재 필터 조건 데이터)

---

## 3. 권한별 차이 (F-13)

| 요소 | 관리자 | 일반 사용자 |
|:---|:---|:---|
| 지자체 필터 | ✅ 표시 (전체 선택 가능) | ❌ 미표시 |
| 테이블 지자체 컬럼 | ✅ 표시 | ❌ 미표시 |
| 대시보드 링크 | ✅ 표시 | ❌ 미표시 |
| 데이터 범위 | 전체 지자체 | 자신의 api_key만 |

---

## 4. API 응답 → 컴포넌트 매핑

### API-06 일별 (API-07 주별, API-08 월별 동일 구조)

| 응답 필드 | → 컴포넌트 |
|:---|:---|
| `stats[].stat_dt` | [D] X축, [G] 날짜 컬럼 |
| `stats[].svc_type` | [D] 시리즈, [E-1] 항목, [G] 서비스 컬럼 |
| `stats[].tot_cnt` | [C] 총호출, [D] Y값, [E-1] 값, [G] 컬럼 |
| `stats[].ok_cnt` | [C] 성공수, [E-2] 값, [G] 컬럼 |
| `stats[].fail_cnt` | [C] 실패수, [E-2] 값, [G] 컬럼 |
| `stats[].avg_ms` | [F] Bar 값, [G] 컬럼 |
| `stats[].gov_name` | [G] 지자체 컬럼 (관리자) |

### 데이터 갱신 흐름

```javascript
async function loadStats() {
    showLoading();
    const tab = getActiveTab(); // daily | weekly | monthly
    const params = buildQueryParams(tab);
    
    const res = await fetch(`/api/stats/${tab}?${params}`, {
        headers: { 'X-API-KEY': apiKey }
    });
    const data = await res.json();
    
    updateSummaryCards(data.stats);
    updateTrendChart(data.stats);
    updateDoughnutCharts(data.stats);
    updateBarChart(data.stats);
    updateDataTable(data.stats);
    hideLoading();
}
```

---

## 5. 스타일 명세

| 항목 | 값 |
|:---|:---|
| 배경 | `#1a1a2e` |
| 카드 | `rgba(255,255,255,0.05)`, `backdrop-filter: blur(10px)`, `border-radius: 12px` |
| 폰트 | `Inter` (Google Fonts) |
| 서비스 배지 | STT: `#3498db`, NLP: `#2ecc71`, TRANSLATE: `#e74c3c`, OCR: `#f39c12`, IMAGE_GEN: `#9b59b6` |
| 탭 활성 | 밑줄 `border-bottom: 2px solid #3498db` |
| 반응형 | ≥1200px: 카드4열+차트풀, 768~1199: 카드2열+차트풀, <768: 1열 스크롤 |

---

## 6. 파일 구조

| 파일 | 역할 |
|:---|:---|
| `templates/stats.html` | Thymeleaf 통계 페이지 |
| `static/css/stats.css` | 통계 페이지 스타일 |
| `static/js/stats.js` | 탭 전환, 필터, 차트, 테이블 로직 |

---
**최종 업데이트**: 2026-05-12
**참조**: UI_04_AI_CALL_STAT_Planning.md, DS_03_API_Specification.md (§3), DS_01_DB_Scheme.md (tb_ai_svc_stat)
