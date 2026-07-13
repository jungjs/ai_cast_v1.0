# -*- coding: utf-8 -*-
"""
txt2img.py
- 입력 텍스트를 PIL로 이미지화
- 폰트: ./fonts 내 NotoSans 계열만 사용
- 색상: .env에서 COLOR_INFORM, COLOR_ANNOUNCE, COLOR_DISASTER 로드
- 로깅: source/logger.py의 logger 사용 (없으면 fallback)
"""
import os, re
from typing import List, Tuple, Optional, Dict
from PIL import Image, ImageDraw, ImageFont
from dotenv import load_dotenv

# -------- logger --------
try:
    from source.logger import logger
except Exception:
    import logging
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    logger = logging.getLogger("txt2img_fallback")

# -------- env (.env) --------
if os.path.exists("./env/.env"):
    load_dotenv("./env/.env")
else:
    load_dotenv(".env")

def _parse_rgb(s: Optional[str], default: Tuple[int,int,int]) -> Tuple[int,int,int]:
    try:
        if not s: return default
        parts = [int(x.strip()) for x in re.split(r"[,\s]+", s) if x.strip()]
        if len(parts) == 3 and all(0 <= v <= 255 for v in parts):
            return (parts[0], parts[1], parts[2])
    except Exception:
        pass
    return default

COLOR_INFORM   = _parse_rgb(os.getenv("COLOR_INFORM"),   (255,242,204))
COLOR_ANNOUNCE = _parse_rgb(os.getenv("COLOR_ANNOUNCE"), (218,227,243))
COLOR_DISASTER = _parse_rgb(os.getenv("COLOR_DISASTER"), (251,229,214))

# -------- fonts (NotoSans only) --------
FONTS_DIR = "./fonts"
LATIN_VAR = os.path.join(FONTS_DIR, "NotoSans-VariableFont_wdth,wght.ttf")
JP_VAR    = os.path.join(FONTS_DIR, "NotoSansJP-VariableFont_wght.ttf")
SC_VAR    = os.path.join(FONTS_DIR, "NotoSansSC-VariableFont_wght.ttf")
TH_VAR    = os.path.join(FONTS_DIR, "NotoSansThai-VariableFont_wdth,wght.ttf")
KM_VAR    = os.path.join(FONTS_DIR, "NotoSansKhmer-VariableFont_wdth,wght.ttf")
DEV_VAR   = os.path.join(FONTS_DIR, "NotoSansDevanagari-VariableFont_wdth,wght.ttf")

def normalize_lang(code: str) -> str:
    if not code: return "ko"
    c = code.strip().lower().replace("_", "-")
    if c.startswith("zh"): return "zh-hans"
    if c.startswith("ja"): return "ja"
    if c.startswith("th"): return "th"
    if c.startswith("km"): return "km"
    if c.startswith("ne"): return "ne"
    if c.startswith("en"): return "en"
    if c.startswith("vi"): return "vi"
    if c.startswith("ru"): return "ru"
    if c.startswith("mn"): return "mn"
    if c.startswith("uz"): return "uz"
    if c.startswith("tl") or c.startswith("fil"): return "fil"
    return "ko"

def clean_text(s: str) -> str:
    if not s: return s
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"[`*_#>]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def _default_font_path(lang_code: str) -> Optional[str]:
    lc = normalize_lang(lang_code)
    if lc == "ja": return JP_VAR
    if lc == "zh-hans": return SC_VAR
    if lc == "th": return TH_VAR
    if lc == "km": return KM_VAR
    if lc == "ne": return DEV_VAR
    return LATIN_VAR

def _get_font(lang_code: str, base_size: int = 56) -> ImageFont.FreeTypeFont:
    candidates = [_default_font_path(lang_code), LATIN_VAR]
    tried = []
    for p in candidates:
        if not p: continue
        tried.append(p)
        try:
            return ImageFont.truetype(p, base_size)
        except Exception:
            continue
    raise RuntimeError(f"No usable font in ./fonts for '{lang_code}'. Tried: {tried}")

# -------- category detection --------
def detect_category(text: str) -> str:
    """
    간단 휴리스틱:
      - 재난(disaster) 키워드가 보이면 disaster
      - 아니면 알림/공지(announce) 키워드가 보이면 announce
      - 아니면 inform
    다국어 키워드 일부 포함.
    """
    if not text:
        return "inform"

    t = text.lower()

    # disaster keywords (ko/en/zh/ja/th/vi/ru/km/ne/uz …)
    kw_disaster = [
        # ko
        "긴급", "재난", "대피", "경보", "속보", "화재", "홍수", "지진", "태풍", "폭설", "폭우", "산사태",
        # en
        "emergency", "evacuate", "evacuation", "alert", "warning", "earthquake", "typhoon", "storm", "flood", "fire",
        # zh-hans
        "紧急", "灾害", "疏散", "警报", "地震", "台风", "洪水", "火灾",
        # ja
        "緊急", "避難", "警報", "地震", "台風", "洪水", "火災",
        # th
        "ฉุกเฉิน", "อพยพ", "เตือนภัย", "แผ่นดินไหว", "พายุ", "น้ำท่วม", "ไฟไหม้",
        # vi
        "khẩn cấp", "sơ tán", "cảnh báo", "động đất", "bão", "lũ lụt", "cháy",
        # ru
        "чрезвычай", "эвакуац", "предупреждени", "землетряс", "тайфун", "наводнен", "пожар",
        # km
        "បន្ទាន់", "ជម្លៀស", "ជូនដំណឹងគ្រោះថ្នាក់", "រញ្ជួយ", "ព្យុះ", "ទឹកជំនន់", "ភ្លើងឆេះ",
        # ne
        "आपतकालीन", "उद्धार", "चेतावनी", "भूकम्प", "आँधी", "बाढी", "आगो",
        # uz
        "favqulodda", "evakuatsiya", "ogohlantirish", "zilzila", "to‘fon", "suv toshqin", "yong‘in",
    ]

    # announce keywords (부고/안내/공지 등 포함)
    kw_announce = [
        # ko
        "알림", "공지", "안내", "부고", "장례", "발인", "예식장", "추모", "애도",
        # en
        "announcement", "notice", "guide", "obituary", "funeral", "wake", "condolence",
        # zh-hans
        "公告", "通知", "告知", "讣告", "葬礼", "出殡",
        # ja
        "告知", "お知らせ", "訃報", "葬儀", "通夜",
        # th
        "ประกาศ", "แจ้งเตือน", "งานศพ",
        # vi
        "thông báo", "cáo phó", "đám tang",
        # ru
        "объявлен", "уведомлен", "некролог", "похорон",
        # km
        "សេចក្តីជូនដំណឹង", "ការរំលែកទុក្ខ", "ពិធីបុណ្យសព",
        # ne
        "सूचना", "शोक पत्र", "अन्त्येष्टि",
        # uz
        "e'lon", "xabarnoma", "motam", "dafn marosimi",
    ]

    def has_kw(s: str, kws: List[str]) -> bool:
        return any(k in s for k in kws)

    if has_kw(t, kw_disaster):
        return "disaster"
    if has_kw(t, kw_announce):
        return "announce"
    return "inform"

def choose_bg_color(category: Optional[str]) -> Tuple[int,int,int]:
    c = (category or "").lower().strip()
    if c == "inform": return COLOR_INFORM
    if c == "announce": return COLOR_ANNOUNCE
    if c == "disaster": return COLOR_DISASTER
    return COLOR_INFORM

# -------- wrapping helpers --------
def _split_by_chars(segment: str, draw: ImageDraw.ImageDraw, font: ImageFont.FreeTypeFont,
                    max_width: int, prefix: str = "") -> List[str]:
    lines: List[str] = []
    line = prefix
    for ch in segment:
        test = line + ch
        if draw.textlength(test, font=font) <= max_width:
            line = test
        else:
            if line:
                lines.append(line.rstrip())
            line = ch
    if line:
        lines.append(line.rstrip())
    return lines

def _wrap_text(text: str, draw: ImageDraw.ImageDraw, font: ImageFont.FreeTypeFont, max_width: int) -> List[str]:
    lines_out: List[str] = []
    for raw in text.splitlines():
        line = raw.rstrip("\r")
        if not line:
            lines_out.append("")
            continue
        parts = re.split(r"(\s+)", line)  # 공백 유지
        cur = ""
        for part in parts:
            test = (cur + part) if cur else part
            if draw.textlength(test, font=font) <= max_width:
                cur = test
            else:
                if cur.strip():
                    lines_out.append(cur.rstrip())
                # 너무 긴 토큰은 문자 단위로 분할
                if not part.isspace() and draw.textlength(part, font=font) > max_width:
                    lines_out.extend(_split_by_chars(part, draw, font, max_width))
                    cur = ""
                else:
                    cur = part.lstrip()
        if cur.strip() or cur == "":
            lines_out.append(cur.rstrip())
    return lines_out

def render_text_to_png(
    text: str,
    out_path_png: str,
    lang_code: str = "ko",
    width: int = 1080,
    base_font_size: int = 56,
    line_gap: int = 8,
    side_padding: int = 40,
    top_bottom_padding: int = 40,
    category: Optional[str] = None,
) -> Dict[str, int]:
    try:
        # category 비지정 시 자동감지
        auto_cat = category or detect_category(text)
        logger.info(f"[txt2img] render start | lang={lang_code} width={width} cat={auto_cat} path={out_path_png}")
        text = clean_text(text or "")
        font = _get_font(lang_code, base_font_size)

        bg_color = choose_bg_color(auto_cat)
        tmp = Image.new("RGB", (width, 100), color=bg_color)
        draw = ImageDraw.Draw(tmp)
        max_text_width = width - side_padding * 2
        wrapped = _wrap_text(text, draw, font, max_text_width)

        # 높이 계산
        heights = []
        for ln in wrapped:
            bbox = draw.textbbox((0, 0), ln if ln else " ", font=font)
            heights.append(bbox[3] - bbox[1])
        text_h = sum(heights) + (len(wrapped)-1)*line_gap if wrapped else 0
        height = top_bottom_padding*2 + max(0, text_h)

        # 실제 렌더
        img = Image.new("RGB", (width, height), color=bg_color)
        draw = ImageDraw.Draw(img)
        y = top_bottom_padding
        for ln, lh in zip(wrapped, heights):
            draw.text((side_padding, y), ln, fill=(0,0,0), font=font)
            y += lh + line_gap

        os.makedirs(os.path.dirname(out_path_png) or ".", exist_ok=True)
        img.save(out_path_png)

        logger.info(f"[txt2img] render done | saved={out_path_png} size=({width},{height})")
        return {"width": width, "height": height}
    except Exception as e:
        logger.error(f"[txt2img] render error: {e}", exc_info=True)
        raise

def text2img(
    text: str,
    lang: str = "ko",
    out_dir: str = "./output",
    filename: Optional[str] = None,
    width: int = 1080,
    category: Optional[str] = None,
    base_font_size: int = 56,
) -> str:
    """
    high-level wrapper. out_dir에 파일 저장 후 경로 반환
    """
    try:
        os.makedirs(out_dir, exist_ok=True)
        safe_name = (filename or "txt2img").strip() or "txt2img"
        if not safe_name.lower().endswith(".png"):
            safe_name += ".png"
        out_path = os.path.join(out_dir, safe_name)

        logger.info(f"[API][REQ] text2img | lang={lang} width={width} req_cat={category} filename={safe_name}")
        render_text_to_png(
            text=text,
            out_path_png=out_path,
            lang_code=lang,
            width=width,
            base_font_size=base_font_size,
            category=category,  # None이면 내부에서 자동 감지
        )
        logger.info(f"[API][RES] text2img | saved={out_path}")
        return out_path
    except Exception as e:
        logger.error(f"[API][ERR] text2img failed: {e}", exc_info=True)
        raise