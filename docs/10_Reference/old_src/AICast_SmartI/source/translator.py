import os
import requests
from langdetect import detect

try:
    from .logger import logger
except ImportError:  # 패키지 외부 실행 대비
    from logger import logger

# 번역 캐시 (메모리 기반, 필요 시 파일/DB로 확장 가능)
translation_cache = {}

def _preview(text, limit: int = 100) -> str:
    if text is None:
        return "None"
    text = str(text).strip()
    return text if len(text) <= limit else f"{text[: limit - 3]}..."

def should_skip_translation(text, to_lang):
    stripped = text.strip()
    if not stripped:
        logger.info("[translator][SKIP] reason=blank target=%s", to_lang)
        return True
    if stripped.isdigit():
        logger.info(
            "[translator][SKIP] reason=numeric target=%s text=%s",
            to_lang,
            stripped,
        )
        return True
    try:
        detected_lang = detect(text)
        if detected_lang == to_lang:
            logger.info(
                "[translator][SKIP] reason=same_language target=%s detected=%s",
                to_lang,
                detected_lang,
            )
            return True
    except Exception:
        logger.error(
            "[translator][ERR] lang_detect_failed target=%s text_preview=%s",
            to_lang,
            _preview(text),
            exc_info=True,
        )
    return False

def call_azure_translate(text, to_lang):
    """Azure Translator API 호출"""
    subscription_key = os.getenv("AZURE_TRANSLATOR_KEY")
    endpoint = os.getenv("AZURE_TRANSLATOR_ENDPOINT")
    location = os.getenv("REGION")
    if not all([subscription_key, endpoint, location]):
        logger.error(
            "[API][ERR] azure_translate env_missing | key=%s endpoint=%s region=%s",
            bool(subscription_key),
            bool(endpoint),
            bool(location),
        )
        raise EnvironmentError("Azure Translator API 환경변수가 누락되었습니다.")

    url = f"{endpoint}/translate?api-version=3.0&to={to_lang}"
    headers = {
        "Ocp-Apim-Subscription-Key": subscription_key,
        "Ocp-Apim-Subscription-Region": location,
        "Content-type": "application/json"
    }
    body = [{"text": text}]

    preview = _preview(text)
    logger.info("[API][REQ] azure_translate | target=%s text=%s", to_lang, preview)
    try:
        response = requests.post(url, headers=headers, json=body, timeout=5)
        response.raise_for_status()
        result = response.json()
        translated = result[0]["translations"][0]["text"]
        logger.info(
            "[API][RES] azure_translate | target=%s text=%s",
            to_lang,
            _preview(translated),
        )
        return translated
    except requests.RequestException:
        logger.error(
            "[API][ERR] azure_translate request_failed | target=%s text=%s",
            to_lang,
            preview,
            exc_info=True,
        )
        raise
    except (KeyError, IndexError, ValueError):
        logger.error(
            "[API][ERR] azure_translate parse_failed | target=%s response=%s",
            to_lang,
            response.text if 'response' in locals() else None,
            exc_info=True,
        )
        raise

def detect_message_type(text: str) -> str:
    prefix = text.strip()[:6]
    if "긴급" in prefix or "재난" in prefix:
        msg_type = "disaster"
    elif "공지" in prefix:
        msg_type = "announce"
    elif "알림" in prefix:
        msg_type = "inform"
    else:
        msg_type = "inform"  # 기본값
    logger.info(
        "[translator][TYPE] detected=%s prefix=%s text=%s",
        msg_type,
        prefix,
        _preview(text),
    )
    return msg_type

def translate_text(text: str, to_langs: list[str], img_village: str = "") -> dict:
    result = {}
    text = text.strip()
    to_langs = sorted(set(lang.lower() for lang in to_langs))

    def cache_key(from_text, to_lang): return f"{from_text}::{to_lang}"
    msg_type = detect_message_type(text)
    logger.info(
        "[API][REQ] translate_text | msg_type=%s targets=%s img_village=%s text=%s",
        msg_type,
        to_langs,
        img_village,
        _preview(text),
    )

    # Step 1. 원문 → 영어 (pivot)
    eng_key = cache_key(text, "en")
    try:
        if should_skip_translation(text, "en"):
            logger.info("[translator][SKIP] reason=pivot_unnecessary target=en")
            intermediate = text
        elif eng_key in translation_cache:
            logger.info("[translator][CACHE] hit | key=%s", eng_key)
            intermediate = translation_cache[eng_key]
        else:
            intermediate = call_azure_translate(text, "en")
            translation_cache[eng_key] = intermediate
            logger.info("[translator][CACHE] store | key=%s", eng_key)
    except Exception:
        logger.error(
            "[API][ERR] translate_text pivot_failed | targets=%s text=%s",
            to_langs,
            _preview(text),
            exc_info=True,
        )
        # 영어조차 번역 실패한 경우: 전체 중단
        return {
            lang: {
                "text": "[영어 번역 실패]",
                "image_path": None,
                "status": "error"
            } for lang in to_langs
        }

    # Step 2. 각 언어별 번역 및 이미지 처리
    for lang in to_langs:
        translation = None
        status = "success"

        if lang == "en":
            translation = intermediate
            logger.info("[translator][USE] direct_pivot | target=en")
        else:
            key = cache_key(intermediate, lang)
            try:
                if should_skip_translation(intermediate, lang):
                    logger.info(
                        "[translator][SKIP] reason=pivot_unnecessary target=%s key=%s",
                        lang,
                        key,
                    )
                    translation = intermediate
                elif key in translation_cache:
                    logger.info(
                        "[translator][CACHE] hit | target=%s key=%s",
                        lang,
                        key,
                    )
                    translation = translation_cache[key]
                else:
                    translation = call_azure_translate(intermediate, lang)
                    translation_cache[key] = translation
                    logger.info(
                        "[translator][CACHE] store | target=%s key=%s",
                        lang,
                        key,
                    )
            except Exception:
                logger.error(
                    "[API][ERR] translate_text fail | target=%s key=%s text=%s",
                    lang,
                    key,
                    _preview(intermediate),
                    exc_info=True,
                )
                translation = "[번역 실패]"
                status = "error"

        # 결과 저장
        result[lang] = {
            "text": translation,
            "status": status
        }
        logger.info(
            "[translator][RES] target=%s status=%s text=%s",
            lang,
            status,
            _preview(translation),
        )

    logger.info(
        "[API][RES] translate_text | targets=%s",
        list(result.keys()),
    )
    return result
