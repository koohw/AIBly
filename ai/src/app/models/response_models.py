from pydantic import BaseModel, Field
from typing import List

class TimelineEvent(BaseModel):
    event: str
    frameIdx: int
    
    class Config:
        json_schema_extra = {
            "example": {
                "event": "차량 충돌",
                "frameIdx": 120
            }
        }

class AnalysisResponse(BaseModel):
    """
    AI 분석 응답 모델
    """
    # 요청에서 가져오는 필드
    userId: int = Field(..., description="사용자 ID")
    videoId: int = Field(..., description="영상 ID")
    fileName: str = Field(..., description="영상 파일명")
    
    # 차량 방향
    carAProgress: str = Field(..., description="A 차량 진행 방향")
    carBProgress: str = Field(..., description="B 차량 진행 방향")
    
    # 과실 비율
    faultA: int = Field(..., description="A 차량 과실 비율(%)", ge=0, le=100)
    faultB: int = Field(..., description="B 차량 과실 비율(%)", ge=0, le=100)
    
    # 추가 필드
    eventTimeline: List[TimelineEvent] = Field(..., description="사고 타임라인 이벤트")
    accidentType: int = Field(..., description="사고 유형 코드")
    damageLocation: str = Field(..., description="손상 위치")
    
    class Config:
        json_schema_extra = {
            "example": {
                "userId": 1,
                "videoId": 123,
                "fileName": "accident_video.mp4",
                "carAProgress": "직진",
                "carBProgress": "좌회전",
                "faultA": 40,
                "faultB": 60,
                "eventTimeline": [
                    {"event": "차량 A 진입", "frameIdx": 50},
                    {"event": "차량 B 진입", "frameIdx": 75},
                    {"event": "충돌 발생", "frameIdx": 120}
                ],
                "accidentType": 2,
                "damageLocation": "전면부"
            }
        }