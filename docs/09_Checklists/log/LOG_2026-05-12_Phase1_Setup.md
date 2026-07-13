# Phase 1: 백엔드 인프라 · 인증 · DB 구축 작업 로그

**작성일시**: 2026-05-12
**작업자**: Antigravity AI Assistant
**기능명**: 백엔드 인프라(Spring Boot 3), DB 구축, 인증 파이프라인
**관련 WBS**: WBS-01 (1.1, 1.2, 1.3)

---

## 1. 적용 체크리스트
- `PHASE_1_DESIGN_CHECKLIST` (인프라 설계 및 인증 보안 검토)
- `PHASE_2_EXECUTE_CHECKLIST` (구현 중 에러 핸들링 및 Null Check 검토)

---

## 2. 작업 내역 및 결과

### 2.1 프로젝트 초기 구성
- **내용**: `pom.xml` 업데이트 (Spring Boot 2.7.18 -> 3.2.5, Java 8 -> 17). JPA, Actuator, Swagger, MariaDB 드라이버 의존성 추가.
- **결과**: `application.yml`에 DB, Hibernate, Actuator, Azure 환경 설정 뼈대 작성. (성공)

### 2.2 데이터베이스 구축
- **내용**: `schema.sql` 작성.
- **결과**: `tb_api_log`, `tb_ai_svc_log`, `tb_ai_svc_stat`, `tb_res_log` 4개 테이블 및 인덱스 생성. `v_api_stat` 통계 뷰 작성 완료. (사용자 요청에 따라 `gov_list`는 DDL에서 제외) (성공)

### 2.3 인증 모듈 및 Repository 구현
- **내용**: `GovList` Entity 및 `GovListRepository` 작성. API 키 기반의 `ApiKeyService`, `ApiKeyAuthFilter` 및 `CorrelationIdFilter` 구현.
- **결과**: 
  - `X-Correlation-ID` 헤더를 통한 추적(NF-03) 구현 완료 (MDC 적용).
  - `X-API-KEY` 인증 파이프라인 구현 완료 (F-06~F-08). 관리자 판별 기능 구현 (F-13).

---

## 3. 체크리스트 준수 사항 및 발견된 이슈
- **[점검] Null Check 및 예외 처리**: `ApiKeyAuthFilter` 내 `apiKey` 파라미터 유무 체크 및 401/403 에러 리스폰스 코드 적용.
- **[점검] 보안 고려사항 (Security)**: API Key는 `gov_list`를 통해서만 유효성 검증을 수행.
- **특이사항**: 기존 설정이 Spring Boot 2 기반이었으나, 모던 아키텍처 지원(Jakarta EE 등)을 위해 3.x로 마이그레이션 적용함.

---

## 4. 관련 SAR
- 현재까지 발견된 주요 결함이나 아키텍처 상의 누락(SAR) 없음.
