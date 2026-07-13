# 시스템 모니터링 대시보드 기획서 (Dashboard Planning)

본 문서는 AI Cast 시스템의 컨테이너 리소스 모니터링 및 API 사용 현황을 실시간으로 표출하는 대시보드(WEB-01, `/dashboard`) 기획안입니다.

---

## 1. 개요

### 1.1. 목적
시스템 운영 상태(리소스 사용량, API 성공률)를 실시간으로 시각화하여 장애 예방 및 운영 효율을 높입니다.

### 1.2. 요구사항 매핑

| 요구사항 | 설명 |
|:---:|:---|
| **F-14** | 컨테이너 리소스(CPU, 메모리, 네트워크, I/O) 모니터링 정보 저장 및 대시보드 표출 |
| **F-15** | 1시간 데이터만 저장, 수집주기 5초 |
| **F-16** | 실시간 변화 추이 그래프 표출, 임계치 초과 시 화면 표출 및 Slack 알림 |
| **F-17** | API 실시간 사용 현황 표출 (요청수, 실패수, 성공수, 실패율, 성공율) |

### 1.3. 주요 사용자
- 시스템 관리자 전용 (관리자 api_key 인증)

### 1.4. 연동 API 및 데이터

| API ID | Endpoint | 데이터 소스 | 용도 |
|:---:|:---|:---|:---|
| API-09 | `GET /api/monitor/resources` | `tb_res_log` | 리소스 모니터링 |
| API-10 | `GET /api/monitor/api-status` | `tb_api_log` | API 사용 현황 |

---

## 2. 주요 측정 지표 (KPI)

### 2.1. 리소스 모니터링 지표 (F-14~F-16)

| 지표명 | 측정 내용 | 임계치 참조 |
|:---|:---|:---|
| **CPU 사용률** | 컨테이너 CPU 사용 비율 (%) | `80_system_resource_cfg.md` |
| **메모리 사용량** | 현재 메모리 / 할당 한도 (MB) | `80_system_resource_cfg.md` |
| **네트워크 I/O** | 수신/송신 바이트 (bytes/sec) | `80_system_resource_cfg.md` |
| **디스크 I/O** | 읽기/쓰기 바이트 (bytes/sec) | `80_system_resource_cfg.md` |

### 2.2. API 사용 현황 지표 (F-17)

| 지표명 | 측정 내용 | 비고 |
|:---|:---|:---|
| **API 요청수** | 당일 전체 API 호출 수 | 시스템 부하 파악 |
| **API 성공수** | 성공 응답(is_ok=true) 수 | 서비스 정상 지표 |
| **API 실패수** | 실패 응답(is_ok=false) 수 | 장애 감지 |
| **API 성공율** | 성공수 / 요청수 × 100 (%) | QoS 지표 |
| **API 실패율** | 실패수 / 요청수 × 100 (%) | 알림 기준 |

---

## 3. 페이지 레이아웃

```
┌──────────────────────────────────────────────────────────┐
│  🖥️ AI Cast 시스템 대시보드                [관리자명]     │
├──────────────────────────────────────────────────────────┤
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ │
│  │API요청수│ │API성공수│ │API실패수│ │ 성공율 │ │ 실패율 │ │
│  │  450   │ │  440   │ │   10   │ │ 97.8% │ │  2.2% │ │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ │
├──────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐ ┌──────────────────────┐      │
│  │  CPU 사용률 (%)       │ │  메모리 사용량 (MB)   │      │
│  │  ████████░░ 82%      │ │  ██████░░░░ 1024/2048│      │
│  │  [실시간 Line Chart]  │ │  [실시간 Line Chart]  │      │
│  │  ──────────────────  │ │  ──────────────────  │      │
│  │  ⚠ WARNING: 82% > 80%│ │                      │      │
│  └──────────────────────┘ └──────────────────────┘      │
├──────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐ ┌──────────────────────┐      │
│  │  네트워크 I/O          │ │  디스크 I/O            │      │
│  │  RX: ▲ 1.2 MB/s      │ │  Read: ▲ 2.0 MB/s    │      │
│  │  TX: ▼ 0.5 MB/s      │ │  Write: ▼ 1.0 MB/s   │      │
│  │  [실시간 Area Chart]  │ │  [실시간 Area Chart]  │      │
│  └──────────────────────┘ └──────────────────────┘      │
├──────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐   │
│  │  최근 API 호출 활동 로그 (Activity Feed)            │   │
│  │  🟢 15:30:02  /api/process_audio  서울시  3200ms  │   │
│  │  🔴 15:29:58  /api/process_img    부산시  Timeout  │   │
│  │  🟢 15:29:45  /api/process_text   대구시  1800ms  │   │
│  │  ...                                             │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

---

## 4. 컴포넌트 상세

### 4.1. API 사용 현황 카드 (F-17)

대시보드 최상단에 5개 카드를 배치합니다.

| 카드 | 데이터 | 색상 | 비고 |
|:---|:---|:---|:---|
| API 요청수 | `summary.tot_req` | Blue | 전일 대비 증감 표시 |
| API 성공수 | `summary.ok_cnt` | Green | |
| API 실패수 | `summary.fail_cnt` | Red | 실패 발생 시 강조 |
| 성공율 | `summary.ok_rate` (%) | Green/Yellow/Red | ≥95: Green, ≥80: Yellow, <80: Red |
| 실패율 | `summary.fail_rate` (%) | Red/Yellow/Green | <5: Green, <20: Yellow, ≥20: Red |

### 4.2. 리소스 실시간 그래프 (F-14~F-16)

4개 영역에 실시간 Line/Area Chart를 배치합니다.

| 영역 | 차트 유형 | X축 | Y축 | 갱신 주기 |
|:---|:---|:---|:---|:---:|
| CPU 사용률 | Line Chart | 시간 (1시간) | % (0~100) | 5초 |
| 메모리 사용량 | Line Chart + 한도 라인 | 시간 | MB | 5초 |
| 네트워크 I/O | Stacked Area Chart | 시간 | bytes/sec (RX/TX) | 5초 |
| 디스크 I/O | Stacked Area Chart | 시간 | bytes/sec (Read/Write) | 5초 |

#### 임계치 및 위험도 표출 (F-16)

##### 위험도 등급 정의 (`80_system_resource_cfg.md` 기반)

| 등급 | 아이콘 | 색상 | CPU | 메모리 | 디스크 | 지속시간 |
|:---|:---:|:---|:---:|:---:|:---:|:---:|
| **정상 (Normal)** | 🟢 | `#2ecc71` (Green) | 0~70% | 0~75% | 0~70% | - |
| **주의 (Warning)** | 🟡 | `#f39c12` (Yellow) | 70~85% | 75~85% | 70~80% | 5분 이상 |
| **경고 (Critical)** | 🟠 | `#e67e22` (Orange) | 85~95% | 85~95% | 80~90% | 5분 이상 |
| **위험 (Emergency)** | 🔴 | `#e74c3c` (Red) | 95%+ | 95%+ | 90%+ | 즉시 |

##### 차트 내 알람 표출

| 표출 방식 | 설명 |
|:---|:---|
| **임계치 경고선** | Warning(노란 점선), Critical(빨간 점선)을 차트에 고정 annotation으로 표시 |
| **영역 배경색** | 현재 값이 위험 구간에 진입 시 해당 구간 배경을 반투명 색상으로 채움 |
| **실시간 게이지** | 각 차트 좌상단에 현재 값 + 위험도 아이콘 표시 (예: `CPU 82% 🟡`) |

##### 화면 알람 표출

```
┌──────────────────────────────────────────────────────────────┐
│  🟢 정상 상태 - 모든 리소스 정상 범위 내                         │  ← 정상 시
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  🟡 주의 - CPU 사용률 78% (5분 지속)                            │  ← Warning 시
│  ├ 해당 리소스 카드 테두리 노란색 점멸                             │
│  └ 카드 상단 ⚠ 배지 표출                                       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  🟠 경고 - 메모리 사용률 92% (Critical)                         │  ← Critical 시
│  ├ 해당 리소스 카드 테두리 오렌지색 점멸                            │
│  ├ 화면 상단에 Alert Banner 고정 표출                            │
│  └ Slack 알림 자동 발송                                        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  🔴 위험 - CPU 사용률 97% (Emergency) - 즉시 대응 필요!          │  ← Emergency 시
│  ├ 전체 대시보드 상단에 빨간색 Alert Banner 고정                   │
│  ├ 경고음(선택적) + 화면 깜빡임 효과                               │
│  ├ Slack 즉시 알림 (디바운싱 없음)                                │
│  └ 해당 리소스 차트 전체 배경 빨간색 반투명                          │
└──────────────────────────────────────────────────────────────┘
```

##### Alert Banner 컴포넌트 구조

```html
<!-- 화면 상단 고정 알람 배너 -->
<div id="alert-banner" class="alert-banner hidden">
    <span class="alert-icon">🟠</span>
    <span class="alert-msg">CPU 사용률 92% - Critical 임계치 초과 (5분 지속)</span>
    <span class="alert-time">15:30:02</span>
    <button class="alert-dismiss">✕</button>
</div>
```

##### 알람 판단 로직 (Frontend)

```javascript
function checkAlerts(alerts) {
    alerts.forEach(alert => {
        const card = document.getElementById(`card-${alert.type.toLowerCase()}`);
        
        // 1. 카드 테두리 색상 변경
        card.classList.remove('normal', 'warning', 'critical', 'emergency');
        card.classList.add(alert.level.toLowerCase());
        
        // 2. 위험도 아이콘 업데이트
        const icon = { NORMAL: '🟢', WARNING: '🟡', CRITICAL: '🟠', EMERGENCY: '🔴' };
        card.querySelector('.status-icon').textContent = icon[alert.level];
        
        // 3. Critical 이상: Alert Banner 표출
        if (alert.level === 'CRITICAL' || alert.level === 'EMERGENCY') {
            showAlertBanner(alert);
        }
    });
}
```

##### Slack 알림 발송 조건

| 등급 | 알림 대상 | 디바운싱 | 비고 |
|:---|:---|:---:|:---|
| **주의 (Warning)** | 화면 표출만 (Slack 미발송) | - | 대시보드 모니터링 |
| **경고 (Critical)** | Slack 알림 발송 | 60초 | 동일 알림 재발송 억제 |
| **위험 (Emergency)** | Slack 즉시 알림 | 없음 | 즉시 발송, 멘션(@channel) 포함 |

> 임계치 수치 및 지속시간은 `80_system_resource_cfg.md` 참조
> 알림 디바운싱은 `application.yml`의 `aicast.monitoring.alerting.debounce-seconds` 설정

### 4.3. 최근 활동 로그 (Activity Feed)

| 항목 | 설명 |
|:---|:---|
| 표시 건수 | 최근 10건 |
| 표시 정보 | 상태 아이콘(🟢/🔴), 시각, 엔드포인트, 지자체명, 소요시간/에러 |
| 갱신 주기 | 10초 |
| 인터랙션 | 클릭 시 Correlation ID 복사 |

---

## 5. 실시간 데이터 갱신 전략

### 5.1. 폴링 방식

| 데이터 | API | 갱신 주기 | 비고 |
|:---|:---|:---:|:---|
| 리소스 모니터링 | API-09 | **5초** | `setInterval(5000)` |
| API 사용 현황 | API-10 | **10초** | 카드 + 활동 로그 |

### 5.2. 구현 방식

```javascript
// 리소스 모니터링 (5초 폴링)
setInterval(async () => {
    const res = await fetch('/api/monitor/resources', {
        headers: { 'X-API-KEY': apiKey }
    });
    const data = await res.json();
    updateResourceCharts(data);
    checkAlerts(data.alerts);  // 임계치 초과 경고 표시
}, 5000);

// API 사용 현황 (10초 폴링)
setInterval(async () => {
    const res = await fetch('/api/monitor/api-status', {
        headers: { 'X-API-KEY': apiKey }
    });
    const data = await res.json();
    updateSummaryCards(data.summary);
    updateActivityFeed(data.recent);
}, 10000);
```

---

## 6. 기술 스택

| 항목 | 선택 | 비고 |
|:---|:---|:---|
| **렌더링** | Spring Boot Thymeleaf (SSR) | 통계 페이지와 통일 |
| **차트 라이브러리** | Chart.js + chartjs-plugin-streaming | 실시간 스트리밍 차트 |
| **CSS 프레임워크** | Bootstrap 5 | Dark Mode 지원 |
| **HTTP 통신** | Fetch API | 폴링 기반 비동기 갱신 |

---

## 7. 디자인 스타일

| 항목 | 설정 |
|:---|:---|
| 테마 | **Dark Mode** + **Glassmorphism** |
| 배경 | `#1a1a2e` (진한 남색) |
| 카드 | `rgba(255,255,255,0.05)` 배경, `backdrop-filter: blur(10px)` |
| 폰트 | Google Fonts `Inter` |
| 경고 색상 | CRITICAL: `#e74c3c`, WARNING: `#f39c12`, NORMAL: `#2ecc71` |
| CPU 차트 | 정상: `#3498db`, 경고: `#f39c12`, 위험: `#e74c3c` |
| 메모리 차트 | 사용량: `#2ecc71`, 한도선: `#e74c3c` 점선 |

---

## 8. 파일 구조 (구현 예정)

```text
src/main/resources/
├── templates/
│   └── dashboard.html          # Thymeleaf 대시보드 페이지
├── static/
│   ├── css/
│   │   └── dashboard.css       # 대시보드 전용 스타일
│   └── js/
│       └── dashboard.js        # 실시간 차트 및 폴링 로직
```

---

## 9. 구현 순서

| 단계 | 작업 | 산출물 |
|:---:|:---|:---|
| 1 | MonitorController 구현 (API-09, 10) | `MonitorController.java` |
| 2 | ResourceMonitorService 구현 (5초 수집) | `ResourceMonitorService.java` |
| 3 | TbResLogRepository / TbApiLogRepository 쿼리 | Repository 클래스 |
| 4 | Thymeleaf 대시보드 레이아웃 구현 | `dashboard.html` |
| 5 | 리소스 실시간 차트 구현 (Chart.js streaming) | `dashboard.js` |
| 6 | API 사용 현황 카드 + 활동 로그 구현 | `dashboard.js` |
| 7 | 임계치 경고 표출 + Slack 연동 | `SlackAlertService.java` |
| 8 | Dark Mode + Glassmorphism 스타일링 | `dashboard.css` |
| 9 | 통합 테스트 | 실시간 갱신 + 알림 검증 |

---
**최종 업데이트**: 2026-05-12
**참조 설계 문서**: DS_02_API_List.md (API-09~10, WEB-01), DS_03_API_Specification.md (§4), DS_04_Class_Design.md (MonitorController, ResourceMonitorService), DS_01_DB_Scheme.md (tb_res_log, tb_api_log), 80_system_resource_cfg.md (임계치)
