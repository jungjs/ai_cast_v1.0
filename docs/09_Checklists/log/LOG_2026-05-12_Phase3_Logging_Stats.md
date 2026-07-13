# Phase 3: 로깅 · 통계 · 모니터링 백엔드 스케줄러 작업 로그

**작성일시**: 2026-05-12
**작업자**: Antigravity AI Assistant
**기능명**: 비동기 로그 적재, 통계 스케줄러, 시스템 리소스 모니터링 및 알림 파이프라인
**관련 WBS**: WBS-01 (3.1, 3.2, 3.3, 3.4)

---

## 1. 적용 체크리스트
- `PHASE_3_EXECUTE_CHECKLIST` (스케줄러 주기 검증, AOP 프록시 타겟 확인, DB 인덱스 유효성 검증)

---

## 2. 작업 내역 및 결과

### 2.1 JPA 엔티티 및 저장소 (DB Schema 반영)
- **내용**: `tb_api_log`, `tb_ai_svc_log`, `tb_ai_svc_stat`, `tb_res_log` 4개 엔티티 구성.
- **결과**: `UUIDGenerator` 기반 기본키 생성 및 `PrePersist`를 이용한 생성일시(`crt_dt`, `col_dt`) 자동 할당 처리 완료. 인덱싱 설정(`@Table(indexes=...)`) 적용 완료.

### 2.2 비동기 로깅 및 AOP
- **내용**: 메인 비즈니스 로직(API 응답 시간)에 영향을 주지 않도록 `@Async` 로깅 적용.
- **결과**: 
  - `ApiLogService` 및 `AiSvcLogService` 작성. 
  - `AiSvcLogAspect`(@Around)를 통해 모든 `com.aicast.client..*.*` 호출에 대해 소요시간 및 예외 상태를 캡처하여 `tb_ai_svc_log`에 기록하도록 구축함 (F-09, F-10).

### 2.3 스케줄러 기반 통계 및 모니터링
- **내용**: 통계 집계 배치 및 시스템 메트릭 수집 스케줄러 구성.
- **결과**:
  - `StatsService`: 매일 00:10에 전일 로그 데이터를 `tb_ai_svc_stat`로 `GROUP BY` 집계 (F-11).
  - `ResourceMonitorService`: `OperatingSystemMXBean`을 이용하여 5초마다 CPU/Memory 등 서버 리소스 지표를 `tb_res_log`에 저장 (F-14). 1시간이 지난 과거 로그를 삭제하는 클리너 스케줄러(`fixedRate = 3600000`) 적용 (F-15).

### 2.4 공통 알림(Slack) 및 예외 처리기
- **내용**: 전역 장애를 감지하고 알림을 쏘는 구조 추가.
- **결과**: 
  - `SlackAlertService`: 60초 디바운싱(Debouncing) 적용. Webhook URL 부재 시 안전한 Mock 처리(에러로그 전환).
  - `GlobalExceptionHandler`: `AzureServiceException` 또는 기타 `Exception` 발생 시 HTTP 에러 반환 및 Slack Webhook(CRITICAL/ERROR 등급) 연계.

### 2.5 모니터링/통계 컨트롤러 (API)
- **내용**: 프론트엔드 제공용 Data API.
- **결과**: `StatsController`(일/주/월), `MonitorController`(리소스 실시간, API 현황 통계) 구현 완료.

---

## 3. 체크리스트 준수 사항 및 발견된 이슈
- **[점검] 비동기 설정 활성화**: `AiCastApplication`에 `@EnableAsync`, `@EnableScheduling`가 정상적으로 어노테이션 추가됨을 확인.
- **[점검] CPU Metric 호환성**: `com.sun.management.OperatingSystemMXBean` 라이브러리를 사용하여 Docker/K8s 환경의 CPU 및 메모리 획득 구조 구축. 향후 cgroup 버전(v1/v2)에 따라 OSHI 라이브러리 연동으로 고도화가 필요할 수 있음.

---

## 4. 관련 SAR
- 현재 발견된 누락이나 아키텍처 상 오류(SAR) 없음.
