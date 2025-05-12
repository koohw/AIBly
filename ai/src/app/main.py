from fastapi import FastAPI
from .routers.endpoints.health_check import router as health_check_router
from .routers.endpoints.report import router as report_router

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


@app.get("/", tags=["기본"], summary="API 상태 확인")
async def root():
    """
    API 서버의 작동 상태를 확인합니다.
    
    Returns:
        dict: API 서버 상태 메시지
    """
    return {"message": "서버 작동중입니다"}

# 라우터 등록
app.include_router(health_check_router)
app.include_router(report_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
