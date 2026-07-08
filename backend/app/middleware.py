import time
from fastapi import Request
from logcontrol import logger

# 서버로 들어오는 모든 통신(신호)을 기록하는 미들웨어 함수
async def log_requests(request: Request, call_next):
    start_time = time.time()

    # 1. 신호 유입 기록
    logger.info(f"Incoming Request: {request.method} {request.url.path}")

    # 실제 서버 로직 처리
    response = await call_next(request)

    # 2. 신호 처리 완료 및 소요 시간 기록
    process_time = time.time() - start_time
    logger.info(f"Completed Request: {request.method} {request.url.path} - Status: {response.status_code} - Time: {process_time:.4f}s")

    return response