# 📋 AI_Cast 에이전트 작업 할당 보드 (Task Board)

이 문서는 총괄 매니저 **Gale**이 수립한 개발 및 검증 태스크 목록입니다.
개발 에이전트 **Dave**와 **Bake (Baker)**는 자신에게 할당된 태스크를 수행하고 결과를 Gale에게 보고해 주십시오.

---

## 👥 에이전트별 태스크 할당

### 👤 Dave - 사용 모델: DeepSeek V4 Flash
- [ ] **T-14. testdata-generator Maven 독립 프로젝트 초기화**
  - 메인 프로그램과 완전히 독립된 디렉토리인 `testdata-generator/`를 신설하고 `pom.xml`을 생성합니다.
  - 빌드 및 실행을 위한 최소한의 라이브러리(Azure Speech SDK, Lombok, JUnit5)만을 세팅합니다.
- [ ] **T-15. 마크다운 파서 및 Azure TTS 연동 컴포넌트 개발**
  - [TtsClient.java](file:///e:/%EB%AA%A8%EB%B9%8C%EB%A6%AC%ED%8B%B0%EC%82%AC%EC%97%85%EB%B3%B8%EB%B6%80/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/2026/vibe%20coding/workspace/testdata-generator/src/main/java/com/aicast/tool/client/TtsClient.java)를 구현하여 Azure Text-to-Speech(TTS) API를 활용해 본문 텍스트를 고품질 한국어 음성(`.wav`)으로 변환 및 저장하는 기능을 개발합니다.
  - 마크다운 파서 모듈을 구현하여 `AI_Cast/docs/80_RawData/마을방송테스트문구.md` 파일로부터 각 시나리오의 [제목] 및 [본문]을 추출하고, 제목에서 특수기호를 제거해 안전한 파일명으로 매핑하는 정규식 추출 로직을 구축합니다.

### 👤 Bake (Baker) - 사용 모델: Big Pickle
- [ ] **T-16. Java AWT 기반 카드뉴스 이미지 렌더러 개발**
  - [ImageRenderer.java](file:///e:/%EB%AA%A8%EB%B9%8C%EB%A6%AC%ED%8B%B0%EC%82%AC%EC%97%85%EB%B3%B8%EB%B6%80/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/2026/vibe%20coding/workspace/testdata-generator/src/main/java/com/aicast/tool/engine/ImageRenderer.java)를 구현하여 Java 표준 AWT Graphics2D 환경을 기반으로 800x800 규격의 카드 뉴스 PNG 이미지를 렌더링하고 내보내는 엔진을 개발합니다.
  - 제목 텍스트에 포함된 키워드(예: '폭염', '태풍', '산불' 등)에 따라 재난(Red/Orange) 또는 일반안내(Blue/Green)의 적절한 카드 배경 테마 컬러를 맵핑하고 본문 텍스트를 줄바꿈하여 카드 중심에 출력하는 렌더링 로직을 개발합니다.
- [ ] **T-17. TestDataGenerator 메인 오케스트레이터 및 연동 검증**
  - [TestDataGenerator.java](file:///e:/%EB%AA%A8%EB%B9%8C%EB%A6%AC%ED%8B%B0%EC%82%AC%EC%97%85%EB%B3%B8%EB%B6%80/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/2026/vibe%20coding/workspace/testdata-generator/src/main/java/com/aicast/tool/TestDataGenerator.java) 메인 진입점을 개발하여 환경설정 파일 로드, 마크다운 파싱, TTS 및 이미지 렌더러 파이프라인 연동을 유기적으로 제어합니다.
  - 생성된 16쌍의 결과물(.wav, .png)이 `AI_Cast/docs/80_RawData/testdata/` 디렉토리 하위에 정상 생성되는지 검증하고 단위 테스트 코드를 작성합니다.

---

## 🔄 협업 프로토콜 준수 사항
1. 작업을 시작할 때 본인 태스크 상태를 진행 중(`[/]`)으로 변경해 주십시오.
2. 테스트 로그 및 결과를 확인한 후 작업이 완료되면 완료(`[x]`) 처리하고 Gale에게 완료 보고서를 제출해 주십시오.
