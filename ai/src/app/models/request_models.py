from pydantic import BaseModel, HttpUrl
from typing import Optional, List, Dict, Any

class AnalysisRequest(BaseModel):
    """AI 분석 요청 모델"""
    user_id: str
    presigned_url: HttpUrl
    metadata: Optional[Dict[str, Any]] = None 