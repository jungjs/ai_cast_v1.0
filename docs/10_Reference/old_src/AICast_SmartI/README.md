# AI Cast Orchestrator

이 프로젝트는 지역 방송 음성 데이터를 받아 **STT → 표준어 정제 → 요약 → 다국어 번역 → 조건부 이미지 생성** 까지를 한 번에 처리하는 FastAPI 서버를 제공합니다. 운영 배포 관점에서 주요 구성과 활용 방법, 그리고 운영자가 알아두어야 할 사항을 정리했습니다.

## 구성 요소

- `main.py`  
  FastAPI 엔트리포인트. `/api/ping`, `/api/latest`, `/api/process_audio` 를 제공합니다.  
  `process_audio` 엔드포인트는 비동기 업로드 파일을 받아 `_safe_pipeline` 으로 전달하고, 파이프라인 결과와 진단 로그를 남깁니다.

- `source/ai_cast.py`  
  Azure Speech STT와 Azure OpenAI를 사용해 음성을 텍스트로 변환하고 표준어/요약을 생성합니다.  
  상세 로그는 `[API][REQ]`, `[API][RES]`, `[API][ERR]` 등의 패턴으로 기록됩니다.

- `source/translator.py`  
  Azure Translator API를 호출하며, 캐시 처리·언어 감지·에러 상황을 세밀하게 로깅합니다.

- `source/txt2img.py`  
  요약 결과를 이미지로 렌더링합니다. `/api/process_audio` 요청 시 번역 언어가 다음 화이트리스트에 포함되면 이미지가 생성됩니다.  
  ```
  베트남(vi), 중국어 간체(zh-hans), 태국(th), 필리핀(fil), 일본(ja),
  몽골(mn), 러시아(ru), 캄보디아(km), 네팔(ne), 우즈베키스탄(uz)
  ```
  생성된 파일은 `output/{요청ID}_{언어코드}.png` 로 저장됩니다.

- `source/logger.py`  
  하루 단위 로테이션 로그를 `logs/` 디렉토리에 남기도록 구성되어 있습니다. 환경 변수로 디렉토리·파일명을 제어할 수 있습니다.

## 파이프라인 요약

1. 업로드된 오디오 파일을 임시 디렉토리에 저장합니다.  
2. `_safe_pipeline()` 이 `process_audio_pipeline` 을 호출해 STT→표준어→요약을 수행합니다.  
3. 요약 텍스트를 요청 언어 목록에 맞춰 번역합니다. Azure Translator가 지원하는 언어는 모두 허용됩니다.  
4. 번역 언어가 화이트리스트인 경우에만 이미지 생성이 추가로 실행됩니다.  
5. 결과는 `id`, `stt`, `normalized`, `summary`, `translations`, `error` 필드로 반환되며, `/api/latest` 에서 가장 최근 결과를 조회할 수 있습니다.

## 서버 구동 및 테스트

## 주요 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `LOG_DIR` | `source/logger.py` 가 로그 파일을 생성할 디렉토리 | `logs` |
| `LOG_FILE` | 로그 파일 베이스 이름(확장자 제외) | `AICast.log` |
| `OUTPUT_DIR` | 번역 이미지가 저장될 디렉토리 | `output` |
| `PORT` | Dockerfile 내 기본 서비스 포트 | `9100` |

Docker 및 운영 환경에서는 위 값을 컨테이너 환경 변수나 `.env` 파일로 주입하면 됩니다. 예: `OUTPUT_DIR=/data/aicast/output`.

## 서버 구동 및 테스트

```bash
# 서버 실행 (로컬 개발)
uvicorn main:app --host 0.0.0.0 --port 9100

# 헬스 체크
curl -s http://127.0.0.1:9100/api/ping | jq .

# 음성 처리 예시 (sample.wav 파일 경로 수정)
curl -s -X POST http://127.0.0.1:9100/api/process_audio \
  -F "file=@/path/to/sample.wav" \
  -F "target_langs=en,ja" | jq .

# 최신 결과 조회
curl -s http://127.0.0.1:9100/api/latest | jq .
```

`jq` 가 없다면 파이프(| jq .)는 제거해도 무방합니다.

## 로그 관련 주의사항

- `source/ai_cast.py` 의 `stt_from_file()` 는 Azure Speech SDK 이벤트를 감시합니다.  
  오디오 입력이 끝나면 SDK가 `CancellationReason.EndOfStream` 원인으로 `recognizer.canceled` 를 호출하는데, 현재 핸들러가 이를 에러 레벨로 기록합니다.  
  ```
  [ERROR] [API][ERR] stt_from_file canceled | reason=CancellationReason.EndOfStream ...
  ```
  이는 실제 장애가 아니라 **정상 종료 시에도 발생하는 로그**입니다. 운영 시 혼동을 방지하려면 EndOfStream 사유에 대해서는 레벨을 낮추거나 메시지를 “정상 종료”로 변경하는 것이 좋습니다.

- 모든 주요 API 호출은 `[API][REQ]`, `[API][RES]`, `[API][ERR]` 포맷으로 남으므로, 특정 `corr=<id>` 값을 따라가면 한 요청의 전체 흐름을 추적할 수 있습니다.

## 운영 시 체크리스트

- 환경 변수: Azure Speech, Azure OpenAI, Azure Translator 의 키/엔드포인트가 모두 필요합니다. 누락 시 `main.py`와 각 모듈에서 에러 로그가 남습니다.
- 로그 파일: `logs/` 디렉토리가 존재하고 쓰기 권한이 있어야 합니다.
- 이미지 출력: `output/` 디렉토리에 대한 쓰기 권한을 확인하세요. 이미지 이름은 `요청 ID + 언어코드` 형식입니다.
- 오류 모니터링: `/api/process_audio` 응답의 `error` 필드를 확인해 파이프라인 어느 단계에서 실패했는지 판단할 수 있습니다. 실패 시에도 `/api/latest` 에 마지막 결과가 저장됩니다.

## Docker 이미지

```
# 이미지 빌드
docker build -t aicast:latest .

# 로그/출력 디렉토리를 호스트 디렉토리에 매핑하여 실행
docker run -d --name aicast \
  -p 9100:9100 \
  -e LOG_DIR=/var/log/aicast \
  -e OUTPUT_DIR=/data/aicast/output \
  -v $(pwd)/logs:/var/log/aicast \
  -v $(pwd)/output:/data/aicast/output \
  aicast:latest
```

- 컨테이너는 9100 포트에서 서비스합니다. `docker run` 시 `-p` 옵션으로 호스트 포트에 매핑하세요.
- `LOG_DIR`, `OUTPUT_DIR` 를 환경 변수로 주입하고, `-v` 옵션으로 호스트 디렉토리를 마운트하면 실행 환경이 바뀌어도 로그/이미지를 원하는 위치에 저장할 수 있습니다. 지정하지 않으면 컨테이너 내부 `/app/logs`, `/app/output` 를 사용합니다.


## 기타

- 기존 `main copy.py` 는 Blob 연동 등 추가 기능을 포함한 레거시 구현입니다. 현재 운영용 API는 `main.py` 를 기준으로 사용합니다.
- 필요 시 테스트나 추가 엔드포인트를 확장할 수 있으며, 로그 패턴을 맞추면 운영 관리자용 모니터링에 일관되게 통합됩니다.
