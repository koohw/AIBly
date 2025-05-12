from fastapi import FastAPI
from .routers.endpoints.health_check import router as health_check_router
from .routers.endpoints.report import router as report_router

app = FastAPI(
    title="AI API",
    description="AI 서비스 API",
    version="0.1.0",
    openapi_url="/api/openapi.json",
    docs_url="/api/docs",
    redoc_url="/api/redoc"
)

@app.get("/")
async def root():
    return {"message": "서버 작동중입니다"}

# 라우터 등록
app.include_router(health_check_router)
app.include_router(report_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
