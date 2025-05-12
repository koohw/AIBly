from pydantic import BaseModel, HttpUrl, Field

class AnalysisRequest(BaseModel):
    """
    AI 분석 요청 모델
    
    사고 영상 분석을 위한 요청 정보를 포함합니다.
    """
    userId: int = Field(..., description="사용자 ID")
    videoId: int = Field(..., description="영상 ID")
    fileName: str = Field(..., description="영상 파일명")
    presignedUrl: HttpUrl = Field(..., description="영상 다운로드를 위한 프리사인드 URL")
    
    class Config:
        schema_extra = {
            "example": {
                "userId": 1,
                "videoId": 123,
                "fileName": "accident_video.mp4",
                "presignedUrl": "https://example.com/download/video.mp4"
            }
        }