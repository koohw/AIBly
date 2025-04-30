from fastapi import FastAPI
from .routers import api_router

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "서버 작동중입니다"}

# 라우터 등록
app.include_router(api_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
