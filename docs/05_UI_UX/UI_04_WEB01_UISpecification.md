# UI 상세설계서 — WEB-01: 시스템 모니터링 대시보드

**화면 ID**: WEB-01 | **URL**: `/dashboard` | **권한**: 관리자 전용
**요구사항**: F-14~F-17 | **연동 API**: API-09, API-10

---

## 1. 화면 구조

```
┌──────────────────────────────────────────────────────────┐
│ [A] 헤더:  🖥️ AI Cast 대시보드    /stats링크   [관리자명]  │
├──────────────────────────────────────────────────────────┤
│ [B] Alert Banner (임계치 초과 시만 표출)                   │
├──────────────────────────────────────────────────────────┤
│ [C] API 현황 카드 (5개 수평)                              │
│  API요청수 | API성공수 | API실패수 | 성공율 | 실패율        │
├──────────────────────────────────────────────────────────┤
│ [D] 리소스 차트 (2×2)                                    │
│  D-1 CPU사용률 | D-2 메모리사용량                          │
│  D-3 네트워크IO | D-4 디스크IO                            │
├──────────────────────────────────────────────────────────┤
│ [E] 최근 활동 로그 (10건)                                 │
└──────────────────────────────────────────────────────────┘
```

---

## 2. 컴포넌트 설계

### [B] Alert Banner (F-16)

| 등급 | 아이콘 | 배경색 | 표출 조건 | Slack |
|:---|:---:|:---|:---|:---:|
| WARNING | 🟡 | `rgba(243,156,18,0.15)` | 리소스 Warning 구간 진입 | ✗ |
| CRITICAL | 🟠 | `rgba(230,126,34,0.15)` | 리소스 Critical 구간 진입 | ✓ (60초 디바운싱) |
| EMERGENCY | 🔴 | `rgba(231,76,60,0.2)` | 리소스 Emergency 구간 | ✓ (즉시, @channel) |

```html
<div id="alert-banner" class="alert-banner hidden">
    <span class="alert-icon">🟠</span>
    <span class="alert-msg">CPU 사용률 92% - Critical (5분 지속)</span>
    <span class="alert-time">15:30:02</span>
    <button class="alert-dismiss" onclick="dismissAlert()">✕</button>
</div>
```

### [C] API 현황 카드 (F-17)

| 카드 ID | 카드명 | 데이터 | 색상 규칙 |
|:---|:---|:---|:---|
| `card-tot-req` | API 요청수 | `summary.tot_req` | Blue 고정 |
| `card-ok-cnt` | API 성공수 | `summary.ok_cnt` | Green 고정 |
| `card-fail-cnt` | API 실패수 | `summary.fail_cnt` | Red (0이면 Green) |
| `card-ok-rate` | 성공율 | `summary.ok_rate` | ≥95%: Green, ≥80%: Yellow, <80%: Red |
| `card-fail-rate` | 실패율 | `summary.fail_rate` | <5%: Green, <20%: Yellow, ≥20%: Red |

각 카드에 전일 대비 증감(▲/▼) 표시.

### [D] 리소스 차트 (F-14~F-16)

| 차트 | 유형 | Y축 | 임계치 라인 | 갱신 |
|:---|:---|:---|:---|:---:|
| D-1 CPU | Line Chart | 0~100% | Warning 75%, Critical 90% (점선) | 5초 |
| D-2 메모리 | Line + 한도선 | 0~`mem_lmt_mb` MB | 한도선 (빨간 수평선) | 5초 |
| D-3 네트워크 | Stacked Area | bytes/sec | - | 5초 |
| D-4 디스크 | Stacked Area | bytes/sec | - | 5초 |

- 차트 좌상단에 현재값 + 위험도 아이콘 표시 (예: `CPU 82% 🟡`)
- 위험 구간 진입 시 차트 배경 반투명 색상 채움
- X축: 최근 1시간 (5초 간격 = 720 데이터포인트)

### [E] 최근 활동 로그

| 요소 | 내용 |
|:---|:---|
| 상태 아이콘 | 🟢 성공 / 🔴 실패 |
| 시각 | `HH:mm:ss` |
| 엔드포인트 | `/api/process_audio` 등 |
| 지자체명 | `gov_name` |
| 소요시간 | `3200ms` 또는 에러 메시지 |
| 인터랙션 | 클릭 → `corr_id` 클립보드 복사 (토스트 알림) |
| 갱신 | 10초 (API-10 응답) |

---

## 3. API 응답 → 컴포넌트 매핑

### API-09 (5초 폴링)

| 응답 필드 | → 컴포넌트 |
|:---|:---|
| `data[].cpu_pct` | [D-1] CPU 차트 |
| `data[].mem_mb`, `mem_lmt_mb` | [D-2] 메모리 차트 |
| `data[].net_rx`, `net_tx` | [D-3] 네트워크 차트 |
| `data[].disk_rd`, `disk_wr` | [D-4] 디스크 차트 |
| `alerts[]` | [B] Alert Banner |

### API-10 (10초 폴링)

| 응답 필드 | → 컴포넌트 |
|:---|:---|
| `summary.*` | [C] API 현황 카드 5개 |
| `recent[]` | [E] 활동 로그 |

---

## 4. 스타일 명세

| 항목 | 값 |
|:---|:---|
| 배경 | `#1a1a2e` |
| 카드 | `rgba(255,255,255,0.05)`, `backdrop-filter: blur(10px)`, `border-radius: 12px` |
| 폰트 | `Inter` (Google Fonts) |
| 반응형 | ≥1200px: 카드5열+차트2×2, 768~1199: 카드3+2+차트1열, <768: 전체1열 |

---

## 5. 파일 구조

| 파일 | 역할 |
|:---|:---|
| `templates/dashboard.html` | Thymeleaf 페이지 |
| `static/css/dashboard.css` | 스타일 |
| `static/js/dashboard.js` | 폴링, 차트, 알람 로직 |

---
**최종 업데이트**: 2026-05-12
**참조**: UI_01_Dashboard_Planning.md, DS_03_API_Specification.md (§4), 80_system_resource_cfg.md
