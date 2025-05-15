from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
import logging
import time
from .routers.endpoints.health_check import router as health_check_router
from .routers.endpoints.report import router as report_router
from .config import Config

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# API 메타데이터 및 설명 추가
app = FastAPI(
    title="사고분석 AI API",
    description="""
    AI를 활용한 자동차 사고 분석 서비스 API
    """,
    version="0.1.0",
    root_path="/ai",
    openapi_tags=[
        {
            "name": "영상 분석",
            "description": "자동차 사고 영상을 분석하여 사고 정보를 추출하는 엔드포인트"
        },
        {
            "name": "상태 확인",
            "description": "시스템 및 GPU 상태를 확인하는 엔드포인트"
        }
    ]
)

# CORS 설정 추가
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 구체적인 도메인으로 제한해야 함
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 요청 처리 시간 로깅 미들웨어
@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    
    # 요청 ID 생성 (실제 환경에서는 UUID 등 사용 권장)
    request_id = f"{int(start_time * 1000)}"
    logger.info(f"Request started: {request_id} - {request.method} {request.url.path}")
    
    try:
        response = await call_next(request)
        process_time = time.time() - start_time
        response.headers["X-Process-Time"] = str(process_time)
        logger.info(f"Request completed: {request_id} - {process_time:.2f}s")
        return response
    except Exception as e:
        logger.error(f"Request failed: {request_id} - {str(e)}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"detail": "서버 내부 오류가 발생했습니다."}
        )

# 요청 검증 오류 핸들러
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.warning(f"요청 검증 오류: {str(exc)}")
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors()}
    )

@app.get("/", tags=["기본"], summary="API 상태 확인")
async def root():
    """
    API 서버의 작동 상태를 확인합니다.
    
    Returns:
        dict: API 서버 상태 메시지
    """
    return {"message": "서버 작동중입니다"}

# 초기화 시 모델 및 리소스 로드
@app.on_event("startup")
async def startup_event():
    logger.info("서버 시작: 리소스 초기화 중...")
    # GPU 환경 설정
    Config.set_gpu_environment()
    logger.info("서버 준비 완료")

# 서버 종료 시 리소스 정리
@app.on_event("shutdown")
async def shutdown_event():
    logger.info("서버 종료: 리소스 정리 중...")
    # 필요한 정리 작업 추가

# 라우터 등록
app.include_router(health_check_router)
app.include_router(report_router)

if __name__ == "__main__": 
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, workers=4)  # workers 수 증가
