# ai_cast.py — origin_ai_cast.py 호환 + 저지연 최적화(FAST_MODE)
# - 기본 동작: STT -> 표준어(LLM) -> 요약(LLM)  (원본과 동일한 품질/흐름)
# - FAST_MODE(환경변수 AI_CAST_FAST_MODE=1): 표준어+요약을 1회 호출로 단축
# - Azure OpenAI (openai==0.28.1) / Azure Speech SDK
# - 음성은 연속 인식(파일 끝까지), 비-PCM/다채널은 STT 진입 전에 16kHz/mono/PCM으로 보정

import os
import wave
import json
import time
import threading
from typing import List, Dict, Any, Optional

from pydub import AudioSegment
import azure.cognitiveservices.speech as speechsdk
import openai
from openai.error import InvalidRequestError

# ------------------------------------------------------------
# 로거: source.logger 가 있으면 사용, 없으면 표준 logging
# ------------------------------------------------------------
try:
    from source.logger import logger  # 프로젝트 표준 로거
except Exception:
    import logging
    logger = logging.getLogger("ai_cast")
    if not logger.handlers:
        h = logging.StreamHandler()
        fmt = logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
        h.setFormatter(fmt)
        logger.addHandler(h)
    logger.setLevel(os.getenv("LOG_LEVEL", "INFO").upper())

# ------------------------------------------------------------
# 환경변수 로드 (필요 시)
# ------------------------------------------------------------
try:
    # 프로젝트 구조에 맞추어 .env 자동 로드가 별도로 있을 수 있으니, 실패해도 무시
    from dotenv import load_dotenv
    load_dotenv("./env/.env.azure")
except Exception:
    pass

# Azure Speech
SPEECH_KEY      = (os.getenv("SPEECH_KEY") or "").strip()
SPEECH_LANGUAGE = (os.getenv("SPEECH_LANGUAGE") or "ko-KR").strip()
REGION          = (os.getenv("REGION") or "").strip()

# Azure OpenAI
openai.api_key     = os.getenv("AZURE_OPENAI_KEY")
openai.api_base    = os.getenv("AZURE_OPENAI_ENDPOINT")          # https://<res>.openai.azure.com
openai.api_type    = os.getenv("AZURE_OPENAI_TYPE", "azure")     # 'azure'
openai.api_version = os.getenv("AZURE_OPENAI_VERSION", "2024-06-01")
deployment_name    = os.getenv("AZURE_OPENAI_DEPLOYMENT")        # 배포명

# 최적화 플래그
FAST_MODE = os.getenv("AI_CAST_FAST_MODE", "0") in ("1", "true", "True")

# ------------------------------------------------------------
# 공통 로그 유틸
# ------------------------------------------------------------
def _preview(text: Optional[str], limit: int = 120) -> str:
    if text is None:
        return "None"
    text = str(text).strip()
    return text if len(text) <= limit else f"{text[: limit - 3]}..."

# ------------------------------------------------------------
# 안정성 체크(로그로만 안내, 런타임엔 상위에서 예외 처리)
# ------------------------------------------------------------
def _chk(name: str, val: Optional[str], secret=False):
    shown = "***" if (secret and val) else (val or "")
    logger.info(f"[ENV] {name}={shown}")

_chk("SPEECH_KEY", SPEECH_KEY, secret=True)
_chk("SPEECH_LANGUAGE", SPEECH_LANGUAGE)
_chk("REGION", REGION)
_chk("AZURE_OPENAI_KEY", openai.api_key, secret=True)
_chk("AZURE_OPENAI_ENDPOINT", openai.api_base)
_chk("AZURE_OPENAI_TYPE", openai.api_type)
_chk("AZURE_OPENAI_VERSION", openai.api_version)
_chk("AZURE_OPENAI_DEPLOYMENT", deployment_name)
_chk("AI_CAST_FAST_MODE", str(FAST_MODE))

# ------------------------------------------------------------
# 원본 프롬프트 (그대로 재사용)
# ------------------------------------------------------------
PROMPT_NORMALIZE_SYSTEM = (
    "이 텍스트는 한국 시골에서 안내방송한 멘트입니다. 사투리 혹은 불분명한 단어가 있을 수 있습니다. "
    "앞 뒤 단어와 문맥, 사투리를 고려해서 표준어로 변경해주세요."
)

PROMPT_SUMMARIZE_SYSTEM = (
    "다음 정보는 한국 시골 마을에서 마을 주민에게 안내방송 하는 음성을 STT처리한 내용입니다. 내용을 요약하여 TV 속보 형식 또는 문자 발송 형태로 제공하고자 함."
    "먼저 무슨 내용인지 구분할 필요가 있습니다. 긴급상황, 자연재해, 재난 관련 내용이면 '긴급', 정부/지자체의 공식적인 내용이면 '공지', "
    "마을 소식 등 단순 알림 내용이면 '알림'으로 가장 앞에 표시해주세요"
    "어르신들을 위한 내용이라 중복적인 내용이 있을 수 있습니다. 중복 제거 해주세요"
    "한번에 여러개의 내용이 있을 수 있습니다. 이 부분은 앞에 숫자를 달아 구분해주세요. "
    "예를들어 '오늘 이장님댁에서 마을잔치가 있습니다. 보건소에서 무료 예방접종 합니다.' 라고 하면 1. 이장님댁에서 마을잔치, 2. 보건소에서 무료 예방접종 과 같이 나눠주세요"
    "숫자, 장소와 같은 정보는 놓치지 않게 포함해주세요."
    "TV 속보 형식으로 나갈 수 있도록 한 문장이 50자 이내로 구성되게 요약해주세요. 정보가 많을 경우 여러 문장으로 구성해주세요."
    "어르신들이 이해하기 쉽도록 간결하고 쉬운 단어를 사용해주세요."
    "정보만 전달해줘. 예를들어 '많은 참여 바랍니다'와 같은 독려 내용은 제외해줘"
)

# ------------------------------------------------------------
# 오디오 유틸 — 원본 그대로 + 보강
# ------------------------------------------------------------
def is_pcm_wav(file_path: str) -> bool:
    try:
        with wave.open(file_path, 'rb') as wav_file:
            return wav_file.getcomptype() == 'NONE'
    except wave.Error:
        return False
    except Exception:
        return False

def convert_to_pcm(input_path: str) -> str:
    logger.info("[AI_CAST][INFO] convert_to_pcm start | src=%s", input_path)
    try:
        sound = AudioSegment.from_file(input_path)
        sound = sound.set_frame_rate(16000).set_channels(1).set_sample_width(2)
        converted_path = input_path.rsplit(".", 1)[0] + "_converted.wav"
        sound.export(converted_path, format="wav")
        logger.info("[AI_CAST][INFO] convert_to_pcm done | dst=%s", converted_path)
        return converted_path
    except Exception:
        logger.error(
            "[API][ERR] convert_to_pcm failed | src=%s",
            input_path,
            exc_info=True,
        )
        raise

def get_audio_duration(file_path: str) -> Optional[float]:
    try:
        with wave.open(file_path, 'r') as wav_file:
            frames = wav_file.getnframes()
            rate = wav_file.getframerate()
            return frames / float(rate) if rate else None
    except Exception:
        return None

# ------------------------------------------------------------
# STT — 연속 인식으로 파일 전체 수집 (origin 구조)
# ------------------------------------------------------------
def stt_from_file(file_path: str) -> str:
    logger.info("[API][REQ] stt_from_file | path=%s", file_path)
    recognized_text: List[str] = []
    converted_path = None
    done = threading.Event()

    try:
        if not is_pcm_wav(file_path):
            converted_path = convert_to_pcm(file_path)
            file_path = converted_path

        # 무음/종료 타임아웃을 약간 늘려 끊김 방지
        speech_config = speechsdk.SpeechConfig(subscription=SPEECH_KEY, region=REGION)
        speech_config.speech_recognition_language = SPEECH_LANGUAGE
        speech_config.set_property(speechsdk.PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "7000")
        speech_config.set_property(speechsdk.PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "1500")

        audio_input = speechsdk.AudioConfig(filename=file_path)
        recognizer = speechsdk.SpeechRecognizer(speech_config=speech_config, audio_config=audio_input)

        def handle_final_result(evt):
            if evt.result.reason == speechsdk.ResultReason.RecognizedSpeech:
                txt = (evt.result.text or "").strip()
                if txt:
                    recognized_text.append(txt)
            elif evt.result.reason == speechsdk.ResultReason.NoMatch:
                logger.warning("[AI_CAST][WARN] stt_no_match | path=%s", file_path)

        def handle_stop(evt):
            done.set()

        def handle_canceled(evt):
            try:
                det = getattr(evt.result, "cancellation_details", None)
                if det:
                    logger.error(
                        "[API][ERR] stt_from_file canceled | reason=%s code=%s details=%s",
                        getattr(det, 'reason', None),
                        getattr(det, 'error_code', None),
                        getattr(det, 'error_details', None),
                    )
                else:
                    logger.error(
                        "[API][ERR] stt_from_file canceled | result_reason=%s",
                        getattr(evt.result, 'reason', None),
                    )
            finally:
                done.set()

        recognizer.recognized.connect(handle_final_result)
        recognizer.session_stopped.connect(handle_stop)
        recognizer.canceled.connect(handle_canceled)

        t0 = time.perf_counter()
        recognizer.start_continuous_recognition()

        # 파일 길이를 기반으로 합리적 대기시간 설정(길이+여유)
        dur = get_audio_duration(file_path) or 60.0
        done.wait(timeout=dur + 5.0)

        recognizer.stop_continuous_recognition()
        took = time.perf_counter() - t0

        result = "\n".join(recognized_text).strip()
        logger.info(
            "[API][RES] stt_from_file | path=%s segs=%d len=%d dur≈%.1fs took=%.2fs text=%s",
            file_path,
            len(recognized_text),
            len(result),
            dur,
            took,
            _preview(result),
        )
        return result
    except Exception:
        logger.error(
            "[API][ERR] stt_from_file failed | path=%s",
            file_path,
            exc_info=True,
        )
        raise
    finally:
        if converted_path and os.path.exists(converted_path):
            try:
                os.remove(converted_path)
                logger.info("[AI_CAST][INFO] cleanup_temp | path=%s", converted_path)
            except Exception:
                logger.warning(
                    "[AI_CAST][WARN] cleanup_temp_failed | path=%s",
                    converted_path,
                    exc_info=True,
                )

# ------------------------------------------------------------
# Azure OpenAI 래퍼 — 0.28.1 (Azure)
# ------------------------------------------------------------
def _chat(messages, temperature=0.3, stage: str = "generic") -> Dict[str, Any]:
    """openai.ChatCompletion.create() 호출 결과 원본 dict를 반환"""
    logger.info(
        "[API][REQ] openai_chat | stage=%s temperature=%.2f messages=%d",
        stage,
        temperature,
        len(messages),
    )
    t0 = time.perf_counter()
    try:
        resp = openai.ChatCompletion.create(
            engine=deployment_name,
            messages=messages,
            temperature=temperature,
        )
        took = time.perf_counter() - t0
        usage = resp.get("usage", {})
        finish = resp["choices"][0].get("finish_reason")
        logger.info(
            "[API][RES] openai_chat | stage=%s finish=%s tokens=%s took=%.2fs",
            stage,
            finish,
            json.dumps(usage) if usage else "{}",
            took,
        )
        return resp
    except Exception:
        logger.error(
            "[API][ERR] openai_chat failed | stage=%s",
            stage,
            exc_info=True,
        )
        raise

def _extract_choice_content(resp: Dict[str, Any], stage: str) -> str:
    choice = resp["choices"][0]
    # content_filter_results 는 있을 수도/없을 수도 있음
    filter_result = choice.get("content_filter_results") or {}
    filtered_categories = []
    for category, data in filter_result.items():
        if isinstance(data, dict) and data.get("filtered") and data.get("severity") != "safe":
            filtered_categories.append(f"{category} (수준:{data.get('severity')})")
    message = choice.get("message", {})
    content = (message.get("content") or "").strip()

    if not content:
        if choice.get("finish_reason") == "content_filter":
            info = ", ".join(filtered_categories) if filtered_categories else "알 수 없음"
            raise RuntimeError(f"콘텐츠 필터 차단 ({stage}) — 사유: {info}")
        raise RuntimeError(f"{stage} 응답에 content가 없습니다.")
    return content

# ------------------------------------------------------------
# 표준어/요약 — 원본 프롬프트 그대로
# ------------------------------------------------------------
def normalize_to_standard_korean(dialect_text: str) -> str:
    try:
        logger.info(
            "[API][REQ] normalize_to_standard_korean | text=%s",
            _preview(dialect_text),
        )
        resp = _chat(
            messages=[
                {"role": "system", "content": PROMPT_NORMALIZE_SYSTEM},
                {"role": "user", "content": f"'{dialect_text}' 를 표준어로 자연스럽게 바꿔줘."},
            ],
            temperature=0.3,
            stage="normalize",
        )
        out = _extract_choice_content(resp, "normalize")
        logger.info(
            "[API][RES] normalize_to_standard_korean | len=%d text=%s",
            len(out),
            _preview(out),
        )
        return out
    except InvalidRequestError as e:
        logger.error(
            "[API][ERR] normalize_to_standard_korean invalid_request | error=%s",
            e,
        )
        raise RuntimeError(str(e))
    except Exception as e:
        logger.error(
            "[API][ERR] normalize_to_standard_korean failed | error=%s",
            e,
            exc_info=True,
        )
        raise RuntimeError(f"{e}")

def summarize_text(standard_text: str) -> str:
    try:
        logger.info(
            "[API][REQ] summarize_text | text=%s",
            _preview(standard_text),
        )
        resp = _chat(
            messages=[
                {"role": "system", "content": PROMPT_SUMMARIZE_SYSTEM},
                {"role": "user",   "content": f"'{standard_text}' 다음 내용을 요약해줘."},
            ],
            temperature=0.3,
            stage="summarize",
        )
        out = _extract_choice_content(resp, "summarize")
        logger.info(
            "[API][RES] summarize_text | len=%d text=%s",
            len(out),
            _preview(out),
        )
        return out
    except InvalidRequestError as e:
        logger.error(
            "[API][ERR] summarize_text invalid_request | error=%s",
            e,
        )
        raise RuntimeError(str(e))
    except Exception as e:
        logger.error(
            "[API][ERR] summarize_text failed | error=%s",
            e,
            exc_info=True,
        )
        raise RuntimeError(f"{e}")

# ------------------------------------------------------------
# FAST_MODE: 표준어+요약 1회 호출 (지연 최소화)
#  - 출력은 JSON: {"normalized":"...", "summary":"..."}
#  - 파싱 실패 시 원본 2단계로 자동 폴백
# ------------------------------------------------------------
_FAST_SYSTEM = (
    "너는 다음 작업을 한 번에 수행한다.\n"
    "1) 입력 문장을 표준어로 자연스럽게 변환한다.\n"
    "2) 변환된 문장을 아래 규칙에 맞춰 요약한다.\n"
    "규칙:\n"
    "- 요약 앞에 유형을 붙인다: 긴급/공지/알림\n"
    "- 숫자·날짜·시간·장소 등 사실 정보 유지, 독려 문구 제거\n"
    "- 여러 건이면 1., 2.로 구분\n"
    "- 문장당 50자 이내, 어르신도 이해 쉬운 단어 사용\n\n"
    "반드시 아래 JSON 하나만 출력해라 (설명/코드블록 금지):\n"
    '{"normalized":"<표준어 문장>","summary":"<요약 문장들>"}'
)

def normalize_and_summarize_fast(text: str) -> Dict[str, str]:
    """1회 호출로 normalized, summary 동시 산출. 실패 시 예외."""
    logger.info("[API][REQ] normalize_and_summarize_fast | text=%s", _preview(text))
    resp = _chat(
        messages=[
            {"role": "system", "content": _FAST_SYSTEM},
            {"role": "user",   "content": text},
        ],
        temperature=0.2,
        stage="fast",
    )
    content = _extract_choice_content(resp, "fast")
    try:
        obj = json.loads(content)
        norm = (obj.get("normalized") or "").strip()
        summ = (obj.get("summary") or "").strip()
        if not norm or not summ:
            raise ValueError("normalized/summary empty")
        logger.info(
            "[API][RES] normalize_and_summarize_fast | norm_len=%d summ_len=%d norm=%s summ=%s",
            len(norm),
            len(summ),
            _preview(norm),
            _preview(summ),
        )
        return {"normalized": norm, "summary": summ}
    except Exception as e:
        logger.error(
            "[API][ERR] normalize_and_summarize_fast parse_failed | error=%s content=%s",
            e,
            _preview(content),
        )
        raise RuntimeError(f"FAST_MODE 파싱 실패: {e}. content={content[:160]}")

# ------------------------------------------------------------
# Public pipeline — 원본과 호환 (필드 동일)
# ------------------------------------------------------------
def process_audio_pipeline(file_path: str) -> Dict[str, Any]:
    """
    Returns:
      {
        "stt": <원문 STT>,
        "normalized": <표준어>,
        "summary": <요약>,
        "error": "" or "<오류메시지>"
      }
    """
    logger.info(
        "[API][REQ] process_audio_pipeline | path=%s fast_mode=%s",
        file_path,
        FAST_MODE,
    )
    stt = stt_from_file(file_path)
    if not stt.strip():
        logger.warning(
            "[API][WARN] process_audio_pipeline empty_stt | path=%s",
            file_path,
        )
        return {
            "stt": "[⚠️ STT 실패]",
            "normalized": "N/A",
            "summary": "N/A",
            "error": "STT error",
        }

    # 빠른 모드: 1회 호출 → 실패 시 2단계로 폴백
    if FAST_MODE:
        try:
            fast = normalize_and_summarize_fast(stt)
            logger.info(
                "[API][RES] process_audio_pipeline | mode=fast stt_len=%d norm_len=%d summ_len=%d",
                len(stt),
                len(fast["normalized"]),
                len(fast["summary"]),
            )
            return {
                "stt": stt,
                "normalized": fast["normalized"],
                "summary": fast["summary"],
                "error": "",
            }
        except Exception as e:
            logger.warning(
                "[API][WARN] process_audio_pipeline fast_mode_fallback | error=%s",
                e,
            )

    # 원본과 동일한 2단계
    try:
        normalized = normalize_to_standard_korean(stt)
    except Exception as e:
        logger.warning(
            "[API][WARN] process_audio_pipeline normalize_failed | error=%s",
            e,
        )
        return {
            "stt": stt,
            "normalized": "[⚠️ 표준어 변환 실패]",
            "summary": "N/A",
            "error": f"normalize error: {e}",
        }

    try:
        summary = summarize_text(normalized)
    except Exception as e:
        logger.warning(
            "[API][WARN] process_audio_pipeline summarize_failed | error=%s",
            e,
        )
        return {
            "stt": stt,
            "normalized": normalized,
            "summary": "[⚠️ 요약 실패]",
            "error": f"summarize error: {e}",
        }

    logger.info(
        "[API][RES] process_audio_pipeline | mode=standard stt_len=%d norm_len=%d summ_len=%d",
        len(stt),
        len(normalized),
        len(summary),
    )
    return {
        "stt": stt,
        "normalized": normalized,
        "summary": summary,
        "error": "",
    }
