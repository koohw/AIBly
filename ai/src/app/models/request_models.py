from pydantic import BaseModel, HttpUrl
from typing import Optional, List, Dict, Any

class AnalysisRequest(BaseModel):
    """AI 분석 요청 모델"""
    userId: int
    videoId: int
    fileName: str
    presignedUrl: HttpUrl