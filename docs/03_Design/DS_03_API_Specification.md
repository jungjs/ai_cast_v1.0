# API 상세 설계서 (API Specification)

본 문서는 AI Cast 시스템의 각 API별 상세 인터페이스 정의를 포함합니다. API 목록은 [API 목록(DS_02)](./DS_02_API_List.md)을 참조하십시오.

## 1. 공통 사항

### 1.1. 인증 방식
- **Header Name**: `X-API-KEY`
- **Description**: `gov_list` 테이블에 기 발급된 API 연동 키 (F-06~F-08).
- **Validation**: `gov_list` 테이블에서 `api_key` 존재 여부 및 `is_active = 1` 조건 확인 (Filter 처리).
- **Fail Case**: 헤더 누락 또는 유효하지 않은 키 → `401 Unauthorized` 반환.

### 1.2. 공통 응답 헤더
- **`X-Correlation-Id`**: 요청 전 과정 추적용 고유 ID (NF-03). 모든 응답에 포함.

### 1.3. 공통 에러 응답 형식

#### ① API Key 인증 및 미인증 에러 (401 Unauthorized)
```json
{
  "corr_id": "a1b2c3d4-...",
  "error": true,
  "status": 401,
  "message": "Invalid or inactive API key"
}
```

#### ② 입력 유효성 검증 실패 에러 (400 Bad Request)
오디오/이미지 등 입력 파일 형식 및 용량 규격을 벗어난 경우 반환되는 공통 명세입니다.
```json
{
  "correlationId": "a1b2c3d4-...",
  "status": "FAILED",
  "errorMessage": "[검증 오류] WAV 포맷 오디오 파일만 업로드할 수 있습니다.",
  "originalText": null,
  "refinedText": null,
  "summary": null,
  "translations": null,
  "imageUrl": null,
  "processingTimesMs": null
}
```

---

## 2. 서비스 파이프라인 API

### 2.1. 시스템 상태 확인 — API-01 (F-01)
- **Endpoint**: `GET /api/ping`
- **인증**: 불필요
- **Response (200 OK)**:
  ```json
  {
    "ok": true,
    "msg": "pong"
  }
  ```

### 2.2. 최신 결과 조회 — API-02 (F-02)
- **Endpoint**: `GET /api/latest`
- **인증**: 필요
- **Response (200 OK)**:
  ```json
  {
    "exists": true,
    "item": {
      "id": "uuid-hex",
      "corr_id": "corr-uuid",
      "stt": "인식된 원문...",
      "refined": "정제된 표준어...",
      "summary": "요약된 내용...",
      "translations": {
        "en": { "text": "...", "img_url": "https://blob.../en.png" },
        "ja": { "text": "...", "img_url": "https://blob.../ja.png" }
      },
      "proc_ms": 4570
    }
  }
  ```

### 2.3. 음성 처리 파이프라인 — API-03 (F-03)
- **Endpoint**: `POST /api/process_audio`
- **인증**: 필요
- **Content-Type**: `multipart/form-data`
- **파이프라인**: WAV → STT(F-18,F-19) → NLP(F-20~F-22) → 번역(F-23~F-25) → 이미지(F-29~F-32) → 업로드(F-33,F-34)
- **Request Parameters**:
    - `file` (File, Required): 오디오 파일
    - `target_langs` (String, Required): 번역 대상 언어 코드 (CSV, 예: `en,ja,vi`)
- **제약 사항 (Constraints)**:
    - **파일 포맷 제한**: Content-Type이 `audio/wav`, `audio/x-wav`, `audio/wave` 이거나 확장자가 `.wav`인 **WAV 파일**만 허용됩니다.
    - **실패 시 응답**: 규격 불일치 시 `400 Bad Request` 코드 및 `[검증 오류] ...` 에러 DTO가 반환됩니다.
    - **대용량 파일 처리**: 내부적으로 Azure Speech SDK의 연속 인식(Continuous Recognition)을 채택하고 있으므로 60초 이상 대용량 파일도 런타임 오류 없이 안정적으로 STT 처리가 완료됩니다.
- **Response (200 OK)**:
  ```json
  {
    "id": "uuid-hex",
    "corr_id": "corr-uuid",
    "stt": "STT 인식 결과 텍스트...",
    "refined": "표준어 변환 텍스트...",
    "summary": "200자 이내 요약...",
    "translations": {
      "en": { "text": "...", "img_url": "https://blob.../en.png" },
      "ja": { "text": "...", "img_url": "https://blob.../ja.png" }
    },
    "category": "disaster",
    "proc_ms": 4570
  }
  ```

### 2.4. 텍스트 처리 파이프라인 — API-04 (F-04)
- **Endpoint**: `POST /api/process_text`
- **인증**: 필요
- **Content-Type**: `multipart/form-data`
- **파이프라인**: TXT → NLP → 번역 → 이미지 → 업로드 (STT 생략)
- **Request Parameters**:
    - `file` (File, Required): TXT 텍스트 파일
    - `target_langs` (String, Required): 번역 대상 언어 코드
- **제약 사항 (Constraints)**:
    - **자동 분할 번역 기능 (Chunking)**: 번역문 글자 수가 Azure AI Translator의 한계치인 10,000자를 초과할 때, 시스템이 내부적으로 **9,000자 단위의 문맥 보존 분할(마침표, 줄바꿈 기호 우선 역탐색)**을 실행하여 자동으로 분할 번역 후 하나의 완성본으로 재취합해 서빙합니다. 따라서 글자 수 상한 예외 없이 무제한 길이 처리가 가능합니다.
- **Response**: API-03과 동일 구조 (`stt` 필드는 null)

### 2.5. 이미지 처리 파이프라인 — API-05 (F-05)
- **Endpoint**: `POST /api/process_img`
- **인증**: 필요
- **Content-Type**: `multipart/form-data`
- **파이프라인**: JPG → OCR(F-26~F-28) → NLP → 번역 → 이미지 → 업로드
- **Request Parameters**:
    - `file` (File, Required): 이미지 파일
    - `target_langs` (String, Required): 번역 대상 언어 코드
- **제약 사항 (Constraints)**:
    - **용량 제한**: 최대 **20MB 이내**의 파일만 분석 가능합니다. (20MB 초과 업로드 차단)
    - **포맷 제한**: `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.webp`, `.pdf`, `.tiff` 확장자만 수용합니다.
    - **실패 시 응답**: 규격/용량 불일치 시 `400 Bad Request` 코드 및 `[검증 오류] ...` 에러 DTO가 반환됩니다.
- **Response (200 OK)**:
  ```json
  {
    "id": "uuid-hex",
    "corr_id": "corr-uuid",
    "ocr": "OCR 추출 텍스트...",
    "refined": "표준어 변환 텍스트...",
    "summary": "200자 이내 요약...",
    "translations": {
      "en": { "text": "...", "img_url": "https://blob.../en.png" }
    },
    "category": "notice",
    "proc_ms": 3200
  }
  ```

---

## 3. AI 호출 통계 API

### 3.1. 일별 호출 통계 조회 — API-06 (F-12, F-13)
- **Endpoint**: `GET /api/stats/daily`
- **인증**: 필요 (관리자: 전체 조회, 일반: 자신의 통계만)
- **Request Query Params**:
    - `start_date` (String, Required): 조회 시작일 (YYYY-MM-DD)
    - `end_date` (String, Required): 조회 종료일 (YYYY-MM-DD)
    - `svc_type` (String, Optional): 서비스 유형 필터 (STT, NLP, TRANSLATE, OCR, IMAGE_GEN)
- **Response (200 OK)**:
  ```json
  {
    "period": { "start": "2026-05-01", "end": "2026-05-11" },
    "stats": [
      {
        "stat_dt": "2026-05-11",
        "gov_name": "서울특별시청",
        "svc_type": "STT",
        "tot_cnt": 120,
        "ok_cnt": 118,
        "fail_cnt": 2,
        "avg_ms": 2340
      }
    ]
  }
  ```

### 3.2. 주별 호출 통계 조회 — API-07 (F-12)
- **Endpoint**: `GET /api/stats/weekly`
- **인증**: 필요
- **Request Query Params**:
    - `year` (Integer, Required): 연도
    - `week` (Integer, Required): 주차 (1~53)
    - `svc_type` (String, Optional): 서비스 유형 필터
- **Response**: API-06과 동일 구조 (일별 합산)

### 3.3. 월별 호출 통계 조회 — API-08 (F-12)
- **Endpoint**: `GET /api/stats/monthly`
- **인증**: 필요
- **Request Query Params**:
    - `year` (Integer, Required): 연도
    - `month` (Integer, Required): 월 (1~12)
    - `svc_type` (String, Optional): 서비스 유형 필터
- **Response**: API-06과 동일 구조 (일별 합산)

---

## 4. 시스템 모니터링 API

### 4.1. 컨테이너 리소스 모니터링 — API-09 (F-14~F-16)
- **Endpoint**: `GET /api/monitor/resources`
- **인증**: 관리자 전용
- **Description**: 최근 1시간 이내 컨테이너 리소스(CPU, 메모리, 네트워크, I/O) 수집 데이터를 조회합니다. 5초 간격 수집 (F-15).
- **Request Query Params**:
    - `ctnr_id` (String, Optional): 컨테이너 ID 필터
- **Response (200 OK)**:
  ```json
  {
    "ctnr_id": "aicast-prod-01",
    "data": [
      {
        "col_dt": "2026-05-12T15:30:00",
        "cpu_pct": 45.2,
        "mem_mb": 1024.5,
        "mem_lmt_mb": 2048.0,
        "net_rx": 1048576,
        "net_tx": 524288,
        "disk_rd": 2097152,
        "disk_wr": 1048576
      }
    ],
    "alerts": [
      {
        "type": "CPU",
        "level": "WARNING",
        "value": 82.5,
        "threshold": 80.0,
        "col_dt": "2026-05-12T15:28:30"
      }
    ]
  }
  ```

### 4.2. API 실시간 사용 현황 — API-10 (F-17)
- **Endpoint**: `GET /api/monitor/api-status`
- **인증**: 관리자 전용
- **Description**: API 실시간 사용 현황 (요청수, 성공/실패수, 성공/실패율)을 조회합니다.
- **Request Query Params**:
    - `period` (String, Optional, Default: `today`): 조회 기간 (`today`, `1h`, `24h`)
- **Response (200 OK)**:
  ```json
  {
    "period": "today",
    "summary": {
      "tot_req": 450,
      "ok_cnt": 440,
      "fail_cnt": 10,
      "ok_rate": 97.78,
      "fail_rate": 2.22
    },
    "by_endpoint": [
      {
        "req_path": "/api/process_audio",
        "tot_req": 200,
        "ok_cnt": 198,
        "fail_cnt": 2,
        "avg_ms": 3200
      },
      {
        "req_path": "/api/process_text",
        "tot_req": 180,
        "ok_cnt": 175,
        "fail_cnt": 5,
        "avg_ms": 1800
      }
    ]
  }
  ```

---

## 5. 상태 코드 정의 (Status Codes)

| Code | Name | Description |
|:---:|:---|:---|
| 200 | OK | 요청 성공 |
| 400 | Bad Request | 필수 파라미터 누락 또는 사전 검증 실패 (WAV 포맷 불일치, 이미지 20MB 초과, 지원외 포맷 유입 시) |
| 401 | Unauthorized | API 키 인증 실패 (키 누락, 미등록, is_active=0) |
| 403 | Forbidden | 권한 부족 (관리자 전용 API 접근 시) |
| 404 | Not Found | 요청한 리소스를 찾을 수 없음 |
| 413 | Payload Too Large | 파일 크기 초과 (이미지 20MB 제한 등, 400 검증 오류와 병행 대응) |
| 429 | Too Many Requests | Azure API 쓰로틀링 발생 |
| 500 | Internal Server Error | 서버 내부 오류 |
| 503 | Service Unavailable | Azure AI 서비스 일시 장애 |

---
**최종 업데이트**: 2026-07-23
**특이사항**: 요구사항 재번호(F-01~F-36) 반영, 인증 API 제거(Filter 처리), AI 호출 통계 API(일/주/월) 신규, 리소스 모니터링·API 사용현황 API 재구성, 이미지 파이프라인(OCR) 응답 구조 추가, DB 약어 컬럼명 반영, 상태 코드 413/429/503 추가, **WAV 오디오 포맷 및 이미지 20MB 용량/허용 확장자 사전 검증 사양(HTTP 400) 추가, 대용량 번역 10,000자 한계 우회 스마트 청크 분할(Auto-Chunking) 연계 사양 기술**

