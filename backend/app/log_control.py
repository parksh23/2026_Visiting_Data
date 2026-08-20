import sys
import logging
from loguru import logger

# 2. Loguru 설정 (콘솔 + TXT 파일 동시 출력 및 자동 삭제)
logger.remove()

# 개발 시 터미널 확인용
logger.add(sys.stdout, level="INFO", format="{time:YYYY-MM-DD HH:mm:ss} | <level>{level: <8}</level> | {message}")

# Render 등 운영 환경이 수집할 수 있도록 로그는 stdout으로만 보낸다.
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
