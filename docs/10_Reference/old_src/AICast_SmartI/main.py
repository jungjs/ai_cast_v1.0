# -*- coding: utf-8 -*-
"""
main.py
- FastAPI 기반 AI Cast API
- 엔드포인트:
    * GET  /api/ping        : 헬스 체크
    * GET  /api/latest      : 마지막 처리 결과 조회
    * POST /api/process_audio
        STT → 표준어 → 요약 → 번역 → (조건부) 이미지 생성 파이프라인 실행
"""

import os
import uuid
import time
import asyncio
import tempfile
from pathlib import Path
from typing import Dict, Any, Optional, List

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv

# ---------------------------------------------------------------------------
# 환경 변수 로드
# ---------------------------------------------------------------------------
ENV_PATH_PRIMARY = Path("./env/.env")
ENV_PATH_FALLBACK = Path(".env")
if ENV_PATH_PRIMARY.exists():
    load_dotenv(ENV_PATH_PRIMARY)
elif ENV_PATH_FALLBACK.exists():
    load_dotenv(ENV_PATH_FALLBACK)

# ---------------------------------------------------------------------------
# 로거
# ---------------------------------------------------------------------------
try:
    from source.logger import logger
except Exception:  # pragma: no cover - fallback
    import logging

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
    )
    logger = logging.getLogger("main_fallback")

# ---------------------------------------------------------------------------
# 내부 모듈
# ---------------------------------------------------------------------------
from source.ai_cast import process_audio_pipeline
from source.translator import translate_text
from source.txt2img import text2img
from source.blob_uploader import upload_to_blob # 새로 추가 

# ---------------------------------------------------------------------------
# 상수 / 전역 상태
# ---------------------------------------------------------------------------
OUTPUT_DIR_ENV = os.getenv("OUTPUT_DIR", "output")
IMAGE_OUTPUT_DIR = Path(OUTPUT_DIR_ENV)
IMAGE_LANG_WHITELIST = {
    "vi",
    "zh-hans",
    "th",
    "fil",
    "ja",
    "mn",
    "ru",
    "km",
    "ne",
    "uz",
}

LATEST_RESULT: Optional[Dict[str, Any]] = None
LATEST_LOCK = asyncio.Lock()

# ---------------------------------------------------------------------------
# 유틸
# ---------------------------------------------------------------------------
def _preview(text: Optional[str], limit: int = 120) -> str:
    if text is None:
        return "None"
    text = str(text).strip()
    return text if len(text) <= limit else f"{text[: limit - 3]}..."


def _parse_langs(raw_langs: Optional[str]) -> List[str]:
    if not raw_langs:
        return ["en"]
    unique: List[str] = []
    for token in raw_langs.split(","):
        code = token.strip().lower()
        if not code:
            continue
        if code not in unique:
            unique.append(code)
    return unique or ["en"]


def _build_translations(
    corr_id: str,
    item_id: str,
    summary_text: str,
    lang_codes: List[str],
) -> Dict[str, Dict[str, Any]]:
    start = time.perf_counter()
    try:
        translate_meta = translate_text(summary_text, lang_codes)
        logger.info(
            "[PIPE][TRANSLATE][RES] corr=%s langs=%s took=%.2fs",
            corr_id,
            list(translate_meta.keys()),
            time.perf_counter() - start,
        )
    except Exception as exc:
        logger.error(
            "[PIPE][TRANSLATE][ERR] corr=%s error=%s",
            corr_id,
            exc,
            exc_info=True,
        )
        raise RuntimeError(f"translate error: {exc}") from exc

    results: Dict[str, Dict[str, Any]] = {}
    for lang in lang_codes:
        entry = translate_meta.get(lang) or {}
        text = entry.get("text") if isinstance(entry, dict) else ""
        status = entry.get("status") if isinstance(entry, dict) else "error"
        if not text and status == "success":
            status = "error"
            text = "[번역 결과 없음]"

        # 이미지 경로 변수 초기화 = None
        image_path = None
        blob_image_path = None

        should_render = (
            status == "success"
            and bool(text)
            and lang in IMAGE_LANG_WHITELIST
        )

        if should_render:
            try:
                IMAGE_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
                filename = f"{item_id}_{lang}.png"

                # 1. local에 이미지 생성
                image_path = text2img(
                    text=text,
                    lang=lang,
                    out_dir=str(IMAGE_OUTPUT_DIR),
                    filename=filename,
                )
                logger.info(
                    "[PIPE][IMG][RES] corr=%s lang=%s path=%s",
                    corr_id,
                    lang,
                    image_path,
                )
                # 2. Blob Storage에 업로드 후, URL 받기
                blob_image_path = upload_to_blob(image_path)
                logger.info(
                    "[PIPE][IMG][RES] corr=%s lang=%s blob_url=%s",
                    corr_id,
                    lang,
                    blob_image_path,
                )
            except Exception as exc:
                image_path = None
                status = "error"
                logger.error(
                    "[PIPE][IMG][ERR] corr=%s lang=%s error=%s",
                    corr_id,
                    lang,
                    exc,
                    exc_info=True,
                )

        results[lang] = {
            "text": text or "",
            "status": status or "error",
            "image_path": blob_image_path,
        }
    return results


def _safe_pipeline(audio_path: str, lang_codes: List[str], corr_id: str) -> Dict[str, Any]:
    """
    STT→표준어→요약→번역→이미지 생성 파이프라인 실행
    """
    start = time.perf_counter()
    item_id = uuid.uuid4().hex
    logger.info(
        "[PIPE][REQ] corr=%s id=%s path=%s langs=%s",
        corr_id,
        item_id,
        audio_path,
        lang_codes,
    )

    payload: Dict[str, Any] = {
        "id": item_id,
        "stt": "",
        "normalized": "",
        "summary": "",
        "translations": {},
        "error": "",
    }

    try:
        # 1. STT -> 표준어 요약
        # Note. 명식적으로 ""으로 기본값 설정을 위해 A or B 구조로 설정 (반환값이 None인 경우도 ""로 처리하기 위해)
        pipe = process_audio_pipeline(audio_path)

        stt_text = pipe.get("stt", "") or ""
        normalized_text = pipe.get("normalized", "") or ""
        summary_text = pipe.get("summary", "") or ""
        pipeline_error = pipe.get("error", "") or ""

        # Note. process_audio_pipeline 결과값 명시적 업데이트 (Fix. 251113 by kyo)
        payload.update(
            {
                "stt" : stt_text,
                "normalized" : normalized_text,
                "summary" : summary_text
            }
        )

        if pipeline_error:
            payload["error"] = pipeline_error
            raise RuntimeError(f"Pipeline error from ai_cast: {pipeline_error}")
        if not summary_text.strip():
            payload["error"] = "summary is empty"
            raise RuntimeError("Summary is empty")


         # 2. 단계 2 : 번역 및 이미지 생성, 업로드
        translations = _build_translations(corr_id, item_id, summary_text, lang_codes)
        payload["translations"] = translations


    # Note. 모든 에러는 아래 exception 블록에서 처리하도록 수정 (25.11.05)
    except Exception as exc:
        if not payload.get("error"): # 이미 에러가 기록되지 않은 상태일 때만 메시지 설정
            payload["error"] = f"pipeline error: {exc}"
        logger.error(
            "[PIPE][ERR] corr=%s id=%s stage=stt_pipeline error=%s",
            corr_id,
            item_id,
            exc,
            exc_info=True,
        )
        
    # 부분 에러 실패 확인 로직 
    if not payload.get("error"): # 전체 파이프라인이 성공했을 때만 부분 실패를 체크
        # 실패한 langs 확인
        failed_langs = [lang for lang, trans in payload.get("translations", {}).items() if trans.get("status") == "error"]
        if failed_langs:
            payload["error"] = f"Partially failed translations for: {', '.join(failed_langs)}"

    duration = time.perf_counter() - start
    if payload.get("error"):
        logger.warning("[PIPE][RES] corr=%s id=%s duration=%.2fs error=%s", corr_id, item_id, duration, payload["error"])
    else:
        logger.info("[PIPE][RES] corr=%s id=%s duration=%.2fs", corr_id, item_id, duration)
    return payload


async def _update_latest(item: Dict[str, Any]) -> None:
    global LATEST_RESULT
    async with LATEST_LOCK:
        LATEST_RESULT = item


def _cleanup_temp(path: Optional[str], corr_id: str) -> None:
    if not path:
        return
    try:
        if os.path.exists(path):
            os.remove(path)
            logger.info("[TMP][DEL] corr=%s path=%s", corr_id, path)
    except Exception:
        logger.warning(
            "[TMP][WARN] corr=%s remove_failed path=%s",
            corr_id,
            path,
            exc_info=True,
        )


# ---------------------------------------------------------------------------
# Pydantic 모델
# ---------------------------------------------------------------------------
class TranslationOut(BaseModel):
    text: str
    status: str
    image_path: Optional[str] = None


class ProcessResponse(BaseModel):
    id: str
    stt: str
    normalized: str
    summary: str
    translations: Dict[str, TranslationOut]
    error: str = ""


# ---------------------------------------------------------------------------
# FastAPI 앱
# ---------------------------------------------------------------------------
app = FastAPI(title="AI Cast Orchestrator API")


@app.get("/api/ping")
def api_ping() -> Dict[str, Any]:
    logger.info("[HTTP][REQ] /api/ping")
    return {"ok": True, "msg": "pong"}


@app.get("/api/latest")
async def api_latest():
    async with LATEST_LOCK:
        if not LATEST_RESULT:
            logger.info("[HTTP][RES] /api/latest | empty")
            return {"exists": False}
        logger.info("[HTTP][RES] /api/latest | id=%s", LATEST_RESULT.get("id"))
        return {"exists": True, "item": LATEST_RESULT}


@app.post("/api/process_audio", response_model=ProcessResponse)
async def api_process_audio(
    file: UploadFile = File(...),
    target_langs: Optional[str] = Form("en,ja,zh-hans,vi,ru"),
):
    corr_id = uuid.uuid4().hex[:8]
    lang_codes = _parse_langs(target_langs)
    logger.info(
        "[HTTP][REQ] /api/process_audio | corr=%s filename=%s langs=%s",
        corr_id,
        file.filename,
        lang_codes,
    )

    suffix = Path(file.filename or "audio").suffix or ".tmp"
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    tmp_path = tmp.name
    try:
        file_bytes = await file.read()
        tmp.write(file_bytes)
        tmp.flush()
        logger.info(
            "[HTTP][INFO] saved_temp_file | corr=%s path=%s size=%d",
            corr_id,
            tmp_path,
            len(file_bytes),
        )
    finally:
        tmp.close()

    try:
        loop = asyncio.get_running_loop()
        payload = await loop.run_in_executor(
            None,
            lambda: _safe_pipeline(tmp_path, lang_codes, corr_id),
        )
        await _update_latest(payload)
        logger.info(
            "[HTTP][RES] /api/process_audio | corr=%s id=%s error=%s",
            corr_id,
            payload.get("id"),
            _preview(payload.get("error")),
        )
        if payload.get("error"):
            logger.warning(
                "[HTTP][WARN] /api/process_audio | corr=%s id=%s error=%s",
                corr_id,
                payload.get("id"),
                payload["error"],
            )
        return payload
    except Exception as exc:
        logger.error(
            "[HTTP][ERR] /api/process_audio | corr=%s error=%s",
            corr_id,
            exc,
            exc_info=True,
        )
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    finally:
        _cleanup_temp(tmp_path, corr_id)
