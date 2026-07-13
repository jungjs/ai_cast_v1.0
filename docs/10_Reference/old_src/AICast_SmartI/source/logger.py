import os
import logging
from logging.handlers import TimedRotatingFileHandler
from datetime import datetime
from dotenv import load_dotenv

# .env 로드
load_dotenv("./env/.env")

# 📦 로그 디렉토리 및 베이스 이름 (기본값 제공)
log_dir = os.getenv("LOG_DIR") or "logs"
log_file_base = os.getenv("LOG_FILE") or "AICast"
os.makedirs(log_dir, exist_ok=True)

# 날짜별 파일 이름 설정: logs/txt2img_YYYYMMDD.log
today_str = datetime.now().strftime("%Y%m%d")
log_filename = os.path.join(log_dir, f"{log_file_base}_{today_str}.log")

# 📋 로깅 설정
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# 파일 핸들러 (하루마다 회전)
file_handler = TimedRotatingFileHandler(
    filename=log_filename,
    when="midnight",
    interval=1,
    backupCount=30,  # 최근 7일치 로그 유지 (원하면 조정 가능)
    encoding="utf-8",
    utc=False
)

file_handler.suffix = "%Y%m%d.log"  # 회전된 파일 이름 포맷

# 콘솔 핸들러
console_handler = logging.StreamHandler()

# 로그 포맷
formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")
file_handler.setFormatter(formatter)
console_handler.setFormatter(formatter)

# 핸들러 등록
logger.addHandler(file_handler)
logger.addHandler(console_handler)
