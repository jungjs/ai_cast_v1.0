from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel
from typing import Dict, Any, Optional, List

import os, sys, uuid, asyncio, tempfile, subprocess, shlex, traceback, time
from datetime import datetime, timedelta, timezone
from pathlib import Path

import logging
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO").upper()
logging.basicConfig(
    level=LOG_LEVEL,
    stream=sys.stdout,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
log = logging.getLogger("main")

from dotenv import load_dotenv
load_dotenv()

# ===== Azure Blob (list/download/SAS) =====
from azure.storage.blob import (
    BlobServiceClient,
    generate_blob_sas,
    BlobSasPermissions,
)

# ===== 검증된 파이프라인 / 번역 =====
# STT -> 표준어 -> 요약
from ai_cast import process_audio_pipeline
# 요약 -> 다국어 번역 (이미지/마을코드 사용 안 함)
from translator import translate_text

# ------------------------------------------------------------------------------
# App
# ------------------------------------------------------------------------------
app = FastAPI(title="Village Broadcast Demo (Azure)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

BASE_DIR = Path(__file__).resolve().parent

# ------------------------------------------------------------------------------
# Blob settings
# ------------------------------------------------------------------------------
BLOB_CONN = os.getenv("AZURE_STORAGE_CONNECTION_STRING")
BLOB_CONTAINER = os.getenv("BLOB_CONTAINER", "alerts")

VIDEO_CONTAINER = os.getenv("VIDEO_CONTAINER") or BLOB_CONTAINER
VIDEO_PREFIX = os.getenv("VIDEO_PREFIX", "")             # e.g., "videos/"
VIDEO_SAS_TTL_MINUTES = int(os.getenv("VIDEO_SAS_TTL_MINUTES", "120"))

blob_service = None
blob_client = None
if BLOB_CONN:
    try:
        blob_service = BlobServiceClient.from_connection_string(BLOB_CONN)
        blob_client = blob_service.get_container_client(BLOB_CONTAINER)
        log.info("[blob] connected container=%s", BLOB_CONTAINER)
    except Exception as e:
        log.warning("[blob] init failed: %s", e)

# ------------------------------------------------------------------------------
# History
# ------------------------------------------------------------------------------
HISTORY: List[Dict[str, Any]] = []
HISTORY_LOCK = asyncio.Lock()

# ------------------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------------------
def _tmp_path(suffix: str) -> str:
    fd, path = tempfile.mkstemp(suffix=suffix)
    os.close(fd)
    return path

def _ffmpeg_to_wav(inpath: str) -> str:
    """아무 입력이든 16kHz mono WAV로 변환"""
    outpath = _tmp_path(".wav")
    cmd = (
       f'ffmpeg -y -i {shlex.quote(inpath)} '
       f'-ac 1 -ar 16000 -acodec pcm_s16le '
       f'{shlex.quote(outpath)}'
    )
    
    try:
        cp = subprocess.run(cmd, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        log.debug("[ffmpeg] to wav ok size=%d", os.path.getsize(outpath))
    except subprocess.CalledProcessError as e:
        detail = e.stderr.decode(errors="ignore")[-500:]
        log.error("[ffmpeg] fail: %s", detail)
        raise HTTPException(status_code=500, detail=f"ffmpeg 변환 실패: {detail}")
    return outpath

def _list_blobs_with_ext(ext: str) -> List[str]:
    if blob_client is None:
        return []
    items = []
    try:
        for b in blob_client.list_blobs():
            name = b.name
            if name.lower().endswith(ext.lower()):
                items.append(name)
        return sorted(items)
    except Exception as e:
        log.warning("[blob] list_blobs failed: %s", e)
        return []

def _download_blob_to_temp(name: str) -> str:
    if blob_client is None:
        raise HTTPException(status_code=400, detail="Blob 설정이 없습니다.")
    blob = blob_client.get_blob_client(name)
    try:
        # 일부 환경에선 exists() 권한 이슈가 있을 수 있어 바로 다운로드 시도
        data = blob.download_blob().readall()
        path = _tmp_path("-" + os.path.basename(name))
        with open(path, "wb") as f:
            f.write(data)
        log.info("[blob] downloaded %s -> %s (%d bytes)", name, path, len(data))
        return path
    except Exception as e:
        log.error("[blob] download failed: %s", e)
        raise HTTPException(status_code=404, detail="해당 blob을 내려받을 수 없습니다.")

def _parse_account_from_conn_str(conn_str: str):
    if not conn_str:
        return None, None
    parts = dict(p.split("=", 1) for p in conn_str.split(";") if "=" in p)
    return parts.get("AccountName"), parts.get("AccountKey")

def _build_item(transcript: str, normalized: str, summary: str, translations: Dict[str, str],
                error_stage: str = "", error_message: str = "") -> Dict[str, Any]:
    return {
        "id": str(uuid.uuid4()),
        "date": datetime.now().isoformat(timespec="seconds"),
        "transcript_raw": transcript,
        "normalized_ko": normalized,
        "summary_ko": summary,
        "translations": translations,
        "error_stage": error_stage,
        "error_message": error_message,
    }

async def _append_history(item: Dict[str, Any]):
    async with HISTORY_LOCK:
        HISTORY.append(item)
        if len(HISTORY) > 1000:
            del HISTORY[:-1000]

def _cleanup(*paths: str):
    for p in paths:
        try:
            if p and os.path.exists(p):
                os.remove(p)
        except Exception:
            pass

# ------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------
class ProcessResponse(BaseModel):
    id: str
    date: str
    transcript_raw: str
    normalized_ko: str
    summary_ko: str
    translations: Dict[str, str]
    error_stage: Optional[str] = ""
    error_message: Optional[str] = ""

# ------------------------------------------------------------------------------
# Static routes
# ------------------------------------------------------------------------------
@app.get("/")
def root():
    return FileResponse(str(BASE_DIR / "mobile.html"))

@app.get("/pc")
def pc():
    return FileResponse(str(BASE_DIR / "pc.html"))

# ------------------------------------------------------------------------------
# Health
# ------------------------------------------------------------------------------
@app.get("/api/ping")
def ping():
    return {"status": "ok"}

# ------------------------------------------------------------------------------
# Video playlist (Blob -> SAS URLs)
# ------------------------------------------------------------------------------
@app.get("/api/video_playlist")
def video_playlist():
    if blob_service is None:
        raise HTTPException(status_code=400, detail="Blob 설정이 없습니다.")
    cont = blob_service.get_container_client(VIDEO_CONTAINER)

    # mp4 수집
    names = []
    try:
        for b in cont.list_blobs(name_starts_with=VIDEO_PREFIX):
            if b.name.lower().endswith(".mp4"):
                names.append(b.name)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"list_blobs 실패: {e}")

    if not names:
        return JSONResponse({"items": []})

    # SAS 생성
    account_name, account_key = _parse_account_from_conn_str(BLOB_CONN)
    if not account_name or not account_key:
        raise HTTPException(status_code=500, detail="AccountName/AccountKey 누락")

    now = datetime.now(timezone.utc)
    expiry = now + timedelta(minutes=VIDEO_SAS_TTL_MINUTES)
    urls = []
    for name in sorted(names):
        sas = generate_blob_sas(
            account_name=account_name,
            container_name=VIDEO_CONTAINER,
            blob_name=name,
            account_key=account_key,
            permission=BlobSasPermissions(read=True),
            expiry=expiry,
        )
        url = f"{cont.url}/{name}?{sas}"
        urls.append(url)

    return JSONResponse({"items": urls})

# ------------------------------------------------------------------------------
# Blob APIs (wav list / process by blob)
# ------------------------------------------------------------------------------
@app.get("/api/list_wavs")
async def list_wavs():
    items = _list_blobs_with_ext(".wav")
    return JSONResponse({"items": items})

def _safe_pipeline(wav_path: str, langs: List[str], corr_id: str) -> Dict[str, Any]:
    """STT→표준어→요약→번역 전체를 실행하고 진단 로그 남김"""
    t0 = time.perf_counter()
    log.info("[corr=%s] pipeline start langs=%s wav=%s", corr_id, langs, wav_path)
    try:
        pipe = process_audio_pipeline(wav_path)
        transcript = pipe.get("stt", "") or ""
        normalized = pipe.get("normalized", "") or ""
        summary = pipe.get("summary", "") or ""
        log.info("[corr=%s] STT len=%d, normalized len=%d, summary len=%d",
                 corr_id, len(transcript), len(normalized), len(summary))

        t1 = time.perf_counter()
        trans_meta = translate_text(summary, langs)
        translations = {k: (v or {}).get("text") for k, v in (trans_meta or {}).items()}
        log.info("[corr=%s] translate langs=%s took=%.2fs",
                 corr_id, list(translations.keys()), time.perf_counter()-t1)

        item = _build_item(transcript, normalized, summary, translations)
        log.info("[corr=%s] pipeline OK total=%.2fs", corr_id, time.perf_counter()-t0)
        return item

    except Exception as e:
        tb = traceback.format_exc(limit=3)
        stage = getattr(e, "stage", "") or "pipeline"
        msg = f"{type(e).__name__}: {e}"
        log.error("[corr=%s] pipeline ERROR stage=%s: %s\n%s", corr_id, stage, e, tb)
        return _build_item("", "", "", {}, error_stage=stage, error_message=msg)

@app.post("/api/process_blob", response_model=ProcessResponse)
async def process_blob(
    name: str = Form(...),
    target_langs: Optional[str] = Form("en,ja,zh-Hans,vi,ru")
):
    corr = str(uuid.uuid4())[:8]
    t0 = time.perf_counter()
    log.info("[corr=%s] process_blob name=%s langs=%s", corr, name, target_langs)

    if not name.strip():
        raise HTTPException(status_code=400, detail="name is required")
    langs = [x.strip() for x in (target_langs or "").split(",") if x.strip()] or ["en"]

    tmp_path = _download_blob_to_temp(name)
    wav_path = _ffmpeg_to_wav(tmp_path)

    item = _safe_pipeline(wav_path, langs, corr)
    await _append_history(item)

    _cleanup(tmp_path)
    if wav_path != tmp_path:
        _cleanup(wav_path)

    log.info("[corr=%s] process_blob done (%.2fs)", corr, time.perf_counter()-t0)
    return JSONResponse(item)

# ------------------------------------------------------------------------------
# Upload/Mic API
# ------------------------------------------------------------------------------
@app.post("/api/process_audio", response_model=ProcessResponse)
async def process_audio(
    file: UploadFile = File(...),
    target_langs: Optional[str] = Form("en,ja,zh-Hans,vi,ru"),
):
    corr = str(uuid.uuid4())[:8]
    t0 = time.perf_counter()
    log.info("[corr=%s] process_audio filename=%s langs=%s", corr, file.filename, target_langs)

    langs = [x.strip() for x in (target_langs or "").split(",") if x.strip()] or ["en"]

    raw_path = _tmp_path("-" + (file.filename or "audio"))
    with open(raw_path, "wb") as f:
        f.write(await file.read())
    wav_path = _ffmpeg_to_wav(raw_path)

    item = _safe_pipeline(wav_path, langs, corr)
    await _append_history(item)

    _cleanup(raw_path, wav_path)
    log.info("[corr=%s] process_audio done (%.2fs)", corr, time.perf_counter()-t0)
    return JSONResponse(item)

# ------------------------------------------------------------------------------
# Latest / History
# ------------------------------------------------------------------------------
@app.get("/api/latest")
async def latest():
    async with HISTORY_LOCK:
        if not HISTORY:
            return JSONResponse({"exists": False})
        last = HISTORY[-1]
        ticker = (last.get("translations") or {}).get("en") or last.get("summary_ko") or ""
        return JSONResponse({"exists": True, "item": last, "ticker": ticker})

@app.get("/api/history")
async def history(limit: int = 50):
    async with HISTORY_LOCK:
        data = HISTORY[-limit:]
    return JSONResponse({"items": data})
