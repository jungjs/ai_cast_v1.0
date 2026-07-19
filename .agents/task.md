# 📋 AI_Cast 에이전트 작업 할당 보드 (Task Board)

이 문서는 총괄 매니저 **Gale**이 수립한 개발 및 검증 태스크 목록입니다.
개발 에이전트 **Dave**와 **Bake (Baker)**는 자신에게 할당된 태스크를 수행하고 결과를 Gale에게 보고해 주십시오.

---

## 👥 에이전트별 태스크 할당

### 👤 Dave - 사용 모델: DeepSeek V4 Flash
- [x] **T-14. testdata-generator Maven 독립 프로젝트 초기화**
- [x] **T-15. 마크다운 파서 및 Azure TTS 연동 컴포넌트 개발**
- [x] **T-18. 토큰/사용량 테이블 스키마 마이그레이션**
- [x] **T-19. JPA Domain Entity 수정 및 필드 추가**
- [x] **T-20. DTO 및 AI 클라이언트 사용량 수집 구현**
- [x] **T-24. WebController에 Playground 페이지 엔드포인트 매핑 추가**
- [x] **T-25. 네비게이션 레이아웃 메뉴 추가 및 common.js 라우팅 추가**
- [x] **T-29. WebController 보완 및 활성 지자체 모델 연동**
- [x] **T-33. WebController의 /stats 엔드포인트 지자체 리스트 주입**
  - [WebController.java](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/java/com/aicast/controller/WebController.java)의 `stats()` 메소드를 수정하여 `Model`에 활성 지자체 목록 `"govList"`를 바인딩하여 반환하게 수정합니다.

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [x] **T-16. Java AWT 기반 카드뉴스 이미지 렌더러 개발**
- [x] **T-17. TestDataGenerator 메인 오케스트레이터 및 연동 검증**
- [x] **T-21. AOP 로깅 Aspect 수정**
- [x] **T-22. 일별 통계 집계 배치 서비스 수정**
- [x] **T-23. 로컬 단위 테스트 및 E2E 통합 테스트 수행**
- [x] **T-26. playground.html 템플릿 마크업 작성**
- [x] **T-27. playground.js 비즈니스 로직 구현**
- [x] **T-30. playground.html 지자체 선택 드롭다운 마크업 보완**
- [x] **T-31. playground.js 동적 API Key 연동 스크립트 보완**
- [x] **T-34. stats.html 지자체 렌더링 마크업 보완 및 stats.js 조회 파라미터 동적 연동**
  - [stats.html](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/templates/stats.html)에서 `#govFilter` 내 하드코딩된 `<option>` 목록을 삭제하고 `th:each` 문법을 사용해 지자체 정보(ID, 명칭)가 출력되도록 동적 템플릿 마크업을 개발합니다.
  - [stats.js](file:///e:/모빌리티사업본부/프로젝트/2026/vibe coding/workspace/AI_Cast/src/main/resources/static/js/stats.js)에서 지자체 필터 변경 시 자동으로 `loadStats()` 조회를 다시 호출하도록 이벤트를 연동하고, 호출 파라미터 `govId`에 선택된 지자체 ID 값이 바인딩되어 날아가도록 수정합니다.

### 👤 Gale (Gale) - 사용 모델: Gemini 3.5 Flash (Medium)
- [x] **T-35. 최종 E2E 검증 및 로컬 서버 컴파일 반영**
  - 로컬 서버 컴파일 기동 후 통계 화면에서 실제 DB 지자체 목록이 미려하게 로딩되는지 확인하고, 조회 시 정상 집계 데이터가 노출되는지 확인합니다.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
