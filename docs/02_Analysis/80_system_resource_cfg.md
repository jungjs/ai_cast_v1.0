# 시스템 리소스 모니터링 한계값 설정 예시

본 문서는 AI Cast 시스템(F-09 요구사항)의 컨테이너 리소스 모니터링을 위한 한계값(Threshold) 설정 가이드 및 업계 표준 예시를 정리합니다.

---

## 1. 리소스별 권장 한계값 (업계 표준)

### 1.1. CPU 사용률

| 등급 | 한계값 | 지속시간 | 설명 |
|:---:|:---:|:---:|:---|
| **정상** | 0 ~ 70% | - | 정상 운영 범위 |
| **주의 (Warning)** | 70 ~ 85% | 5분 이상 지속 | 모니터링 강화 필요, 대시보드 표시 |
| **경고 (Critical)** | 85 ~ 95% | 5분 이상 지속 | 관리자 알림 발송, 스케일링 검토 |
| **위험 (Emergency)** | 95% 이상 | 즉시 | 즉시 관리자 알림, 긴급 대응 필요 |

> **참고**: 순간적인 스파이크는 무시하고, **지속적인(Sustained)** 사용률에 기반하여 판단합니다.
> CPU 쓰로틀링(Throttling) 비율이 25% 이상이면 리소스 부족으로 판단합니다.

### 1.2. 메모리 사용률

| 등급 | 한계값 | 지속시간 | 설명 |
|:---:|:---:|:---:|:---|
| **정상** | 0 ~ 75% | - | 정상 운영 범위 |
| **주의 (Warning)** | 75 ~ 85% | 5분 이상 지속 | 메모리 누수 점검 필요 |
| **경고 (Critical)** | 85 ~ 95% | 3분 이상 지속 | 관리자 알림 발송, OOM 위험 |
| **위험 (Emergency)** | 95% 이상 | 즉시 | OOMKilled 임박, 즉시 대응 |

> **참고**: JVM 기반 애플리케이션은 힙 메모리(`jvm_memory_used_bytes / jvm_memory_max_bytes`)를 별도 모니터링합니다.
> 과도한 Swap 사용은 성능 저하 원인이므로 Active Swap 사용량도 함께 감시합니다.

### 1.3. 디스크 사용률

| 등급 | 한계값 | 지속시간 | 설명 |
|:---:|:---:|:---:|:---|
| **정상** | 0 ~ 70% | - | 정상 운영 범위 |
| **주의 (Warning)** | 70 ~ 80% | 10분 이상 지속 | 로그 정리 및 불필요 파일 삭제 검토 |
| **경고 (Critical)** | 80 ~ 90% | 5분 이상 지속 | 관리자 알림 발송, 디스크 확장 검토 |
| **위험 (Emergency)** | 90% 이상 | 즉시 | 시스템 장애 가능, 즉시 대응 |

> **추가 지표**: 디스크 레이턴시(ms)와 I/O Wait 비율도 함께 모니터링합니다.
> `predict_linear` 함수로 24시간 내 디스크 풀 예측 알림을 구성할 수 있습니다.

### 1.4. 네트워크 I/O

| 등급 | 한계값 | 지속시간 | 설명 |
|:---:|:---:|:---:|:---|
| **정상** | 기준선 대비 정상 범위 | - | 베이스라인 기반 판단 |
| **주의 (Warning)** | 기준선 대비 150% 이상 | 5분 이상 지속 | 트래픽 증가 원인 분석 |
| **경고 (Critical)** | 기준선 대비 200% 이상 또는 대역폭 80% 도달 | 5분 이상 지속 | DDoS 또는 이상 트래픽 의심 |
| **위험 (Emergency)** | 패킷 손실률 > 1% 또는 대역폭 포화 | 즉시 | 네트워크 장애 가능, 즉시 대응 |

> **참고**: 네트워크는 절대값보다 **베이스라인 대비 편차**로 판단하는 것이 효과적입니다.
> 1~2주간 정상 트래픽 패턴을 수집한 후 기준선을 설정합니다.

---

## 2. AI Cast 시스템 적용 설정 예시

### 2.1. Spring Boot Actuator 설정 (`application.yml`)

Spring Boot Actuator는 메트릭을 **노출**하는 역할이며, 알림 로직은 외부 모니터링 도구에서 처리합니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  health:
    diskspace:
      threshold: 10485760  # 10MB - 디스크 여유 공간이 이 값 이하이면 DOWN 상태
```

### 2.2. AI Cast 전용 리소스 한계값 설정 (`application.yml`)

```yaml
# AI Cast 시스템 리소스 모니터링 한계값 설정
aicast:
  monitoring:
    # 수집 주기 (초)
    collection-interval: 30

    thresholds:
      cpu:
        warning: 75       # CPU 사용률 Warning (%)
        critical: 90      # CPU 사용률 Critical (%)
        duration: 300     # 지속시간 판단 기준 (초, 5분)

      memory:
        warning: 80       # 메모리 사용률 Warning (%)
        critical: 90      # 메모리 사용률 Critical (%)
        duration: 300     # 지속시간 판단 기준 (초, 5분)

      disk:
        warning: 75       # 디스크 사용률 Warning (%)
        critical: 90      # 디스크 사용률 Critical (%)
        duration: 600     # 지속시간 판단 기준 (초, 10분)

      network:
        warning: 150      # 기준선 대비 비율 Warning (%)
        critical: 200     # 기준선 대비 비율 Critical (%)
        duration: 300     # 지속시간 판단 기준 (초, 5분)

    # 알림 설정
    alerting:
      enabled: true
      debounce-seconds: 60    # 동일 알림 재발송 억제 시간 (초)
      channels:
        - type: log             # 로그 기록 (항상 활성)
          severity: warning
        - type: email           # 이메일 알림
          severity: critical
```

### 2.3. Kubernetes 컨테이너 리소스 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: aicast-api
spec:
  template:
    spec:
      containers:
      - name: aicast-api
        image: aicast-api:latest
        resources:
          # 일반적인 사용량 (스케줄러 참조용)
          requests:
            memory: "512Mi"
            cpu: "500m"
          # 최대 허용 한계 (초과 시 Throttle/OOMKill)
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### 2.4. Prometheus 알림 규칙 예시

```yaml
groups:
  - name: aicast_resource_alerts
    rules:
      # --- CPU 알림 ---
      - alert: AICastHighCpuUsage
        expr: |
          100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
          service: aicast
        annotations:
          summary: "AI Cast CPU 사용률 경고 ({{ $labels.instance }})"
          description: "CPU 사용률이 5분 이상 85%를 초과했습니다. 현재: {{ $value | printf \"%.1f\" }}%"

      - alert: AICastCriticalCpuUsage
        expr: |
          100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 95
        for: 3m
        labels:
          severity: critical
          service: aicast
        annotations:
          summary: "AI Cast CPU 사용률 위험 ({{ $labels.instance }})"
          description: "CPU 사용률이 3분 이상 95%를 초과했습니다. 즉시 확인이 필요합니다."

      # --- 메모리 알림 ---
      - alert: AICastHighMemoryUsage
        expr: |
          (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85
        for: 5m
        labels:
          severity: warning
          service: aicast
        annotations:
          summary: "AI Cast 메모리 사용률 경고 ({{ $labels.instance }})"
          description: "메모리 사용률이 5분 이상 85%를 초과했습니다."

      - alert: AICastCriticalMemoryUsage
        expr: |
          (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 95
        for: 3m
        labels:
          severity: critical
          service: aicast
        annotations:
          summary: "AI Cast 메모리 사용률 위험 ({{ $labels.instance }})"
          description: "OOM 위험! 메모리 사용률이 95%를 초과했습니다."

      # --- JVM 힙 메모리 알림 (Spring Boot 전용) ---
      - alert: AICastJvmHeapHigh
        expr: |
          jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
          service: aicast
        annotations:
          summary: "AI Cast JVM 힙 메모리 사용률 경고"
          description: "JVM 힙 메모리 사용률이 85%를 초과했습니다."

      # --- 디스크 알림 ---
      - alert: AICastLowDiskSpace
        expr: |
          (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100 > 85
        for: 10m
        labels:
          severity: critical
          service: aicast
        annotations:
          summary: "AI Cast 디스크 공간 부족 ({{ $labels.instance }})"
          description: "디스크 사용률이 85%를 초과했습니다."

      - alert: AICastDiskWillFillSoon
        expr: |
          predict_linear(node_filesystem_avail_bytes{mountpoint="/"}[1h], 24 * 3600) < 0
        for: 1h
        labels:
          severity: critical
          service: aicast
        annotations:
          summary: "AI Cast 디스크 24시간 내 풀 예상 ({{ $labels.instance }})"
          description: "현재 추세로 24시간 내에 디스크가 가득 찰 것으로 예측됩니다."

      # --- 네트워크 알림 ---
      - alert: AICastHighNetworkTraffic
        expr: |
          rate(node_network_receive_bytes_total[5m]) > 1e+08
        for: 5m
        labels:
          severity: warning
          service: aicast
        annotations:
          summary: "AI Cast 네트워크 트래픽 이상 ({{ $labels.instance }})"
          description: "네트워크 수신 트래픽이 100MB/s를 초과했습니다."
```

---

## 3. Azure Monitor 기반 설정 (Azure Container Apps)

AI Cast 시스템은 Azure Cloud 환경을 사용하므로, Azure Monitor의 메트릭 알림도 활용 가능합니다.

### 3.1. Azure 메트릭 알림 설정 예시

| 메트릭 | 연산자 | 한계값 | 집계 방식 | 집계 기간 | 심각도 |
|:---|:---:|:---:|:---:|:---:|:---:|
| CpuPercentage | Greater than | 80% | Average | 5분 | Sev 2 (Warning) |
| CpuPercentage | Greater than | 95% | Average | 3분 | Sev 0 (Critical) |
| MemoryPercentage | Greater than | 85% | Average | 5분 | Sev 2 (Warning) |
| MemoryPercentage | Greater than | 95% | Average | 3분 | Sev 0 (Critical) |

### 3.2. 정적 vs 동적 한계값

- **정적 한계값 (Static)**: 고정 값 설정 (예: CPU > 80%). 예측 가능한 워크로드에 적합.
- **동적 한계값 (Dynamic)**: Azure ML 기반으로 과거 데이터를 분석하여 자동 계산. 트래픽 패턴이 변동적인 경우 적합. (최소 3일 이상의 데이터 필요)

---

## 4. 알림 운영 베스트 프랙티스

### 4.1. 알림 피로도(Alert Fatigue) 방지

| 전략 | 설명 |
|:---|:---|
| **디바운싱(Debouncing)** | 조건이 N분 이상 지속될 때만 알림 발생 (예: `for: 5m`) |
| **중복 억제** | 동일 알림 재발송 억제 시간 설정 (예: 60초) |
| **단계별 알림** | Warning → 대시보드/Slack, Critical → 이메일/SMS |
| **정기 리뷰** | 월 1회 알림 로그 검토, 노이즈 알림 튜닝 |

### 4.2. 상관 분석 (Metric Correlation)

단일 지표가 아닌 복합 지표 기반으로 근본 원인을 파악합니다.

| 패턴 | CPU | I/O Wait | 메모리 | 원인 분석 |
|:---|:---:|:---:|:---:|:---|
| 앱 과부하 | 높음 | 낮음 | 보통 | 애플리케이션 로직 최적화 필요 |
| 스토리지 병목 | 높음 | 높음 | 보통 | 디스크 I/O 성능 개선 필요 |
| 메모리 부족 | 낮음 | 낮음 | 높음 | 메모리 증설 또는 누수 수정 필요 |
| 전반적 과부하 | 높음 | 높음 | 높음 | 스케일 아웃 또는 리소스 증설 필요 |

### 4.3. 베이스라인 설정 절차

1. **데이터 수집**: 1~2주간 정상 운영 데이터 수집
2. **패턴 분석**: 피크/오프피크 시간대 리소스 사용 패턴 파악
3. **기준선 설정**: 평균 + 2σ(표준편차)를 Warning 기준으로 설정
4. **주기적 갱신**: 분기 1회 베이스라인 재산정

---

## 5. AI Cast 시스템 권장 한계값 요약

AI Cast 시스템 특성(오디오/텍스트/이미지 처리 파이프라인)을 고려한 권장 설정입니다.

| 리소스 | Warning | Critical | 지속시간 (for) | 비고 |
|:---|:---:|:---:|:---:|:---|
| **CPU** | 75% | 90% | 5분 | AI API 호출 시 일시적 스파이크 허용 |
| **메모리** | 80% | 90% | 5분 | JVM 힙 메모리 별도 모니터링 |
| **JVM 힙** | 80% | 90% | 5분 | GC 빈도와 함께 분석 |
| **디스크** | 75% | 90% | 10분 | 로그 및 임시 파일 자동 정리 연계 |
| **네트워크** | 기준선 150% | 기준선 200% | 5분 | 베이스라인 기반 동적 판단 |
| **CPU 쓰로틀링** | 15% | 25% | 5분 | 컨테이너 리소스 한계 초과 지표 |

---

## 6. 관련 요구사항 매핑

| 요구사항 ID | 설명 | 관련 섹션 |
|:---:|:---|:---|
| F-09 | 컨테이너 리소스(CPU, 메모리, 네트워크, I/O) 모니터링 | 전체 |
| F-10 | 리소스 사용현황 조회 웹 인터페이스 | §2.1 Actuator, §3 Azure Monitor |
| NF-05 | 장애 발생 시 관리자 알림 | §2.2 알림 설정, §4.1 알림 운영 |

---

**최종 업데이트**: 2026-05-12
**작성 기준**: 업계 표준 및 Azure/Kubernetes/Prometheus 공식 가이드 참조
