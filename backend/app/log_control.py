import os
import sys
import logging
from loguru import logger

# 1. 저장 경로 설정
LOG_DIR = "/data/logs" if os.environ.get("RENDER") else "./logs"
os.makedirs(LOG_DIR, exist_ok=True)
LOG_FILE_PATH = os.path.join(LOG_DIR, "server_logs.txt")

# 2. Loguru 설정 (콘솔 + TXT 파일 동시 출력 및 자동 삭제)
logger.remove()

# 개발 시 터미널 확인용
logger.add(sys.stdout, level="INFO", format="{time:YYYY-MM-DD HH:mm:ss} | <level>{level: <8}</level> | {message}")

# TXT 파일 저장 (50MB 도달 시 분리, 30일 경과 시 자동 삭제)
logger.add(
    LOG_FILE_PATH,
    rotation="50 MB",
    retention="30 days",
    compression="zip",
    encoding="utf-8",
    level="INFO",
    format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {message}"
)

# 3. Uvicorn/FastAPI 기본 시스템 로그 가로채기
class InterceptHandler(logging.Handler):
    def emit(self, record):
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno

        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1

        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())

def setup_logging():
    logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)
    for logger_name in ("uvicorn", "uvicorn.access", "uvicorn.error", "fastapi"):
        mod_logger = logging.getLogger(logger_name)
        mod_logger.handlers = [InterceptHandler()]
        mod_logger.propagate = False