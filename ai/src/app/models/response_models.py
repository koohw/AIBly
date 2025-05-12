from pydantic import BaseModel, Field

class AnalysisResponse(BaseModel):
    """
    AI 분석 응답 모델
    
    사고 영상 분석 결과 정보를 포함합니다.
    """
    userId: int = Field(..., description="사용자 ID")
    videoId: int = Field(..., description="영상 ID")
    fileName: str = Field(..., description="영상 파일명")
    accidentPlace: str = Field(..., description="사고 발생 장소")
    accidentFeature: str = Field(..., description="사고 장소 특징(도로 상태, 신호등 등)")
    carAProgress: str = Field(..., description="A 차량 진행 방향")
    carBProgress: str = Field(..., description="B 차량 진행 방향")
    faultA: int = Field(..., description="A 차량 과실 비율(%)", ge=0, le=100)
    faultB: int = Field(..., description="B 차량 과실 비율(%)", ge=0, le=100)
    title: str = Field(..., description="사고 제목")
    laws: str = Field(..., description="관련 법조문")
    precedents: str = Field(..., description="관련 판례")
    
    class Config:
        schema_extra = {
            "example": {
                "userId": 1,
                "videoId": 123,
                "fileName": "accident_video.mp4",
                "accidentPlace": "서울시 강남구 테헤란로",
                "accidentFeature": "4차선 도로, 신호등 있음",
                "carAProgress": "직진",
                "carBProgress": "우회전",
                "faultA": 80,
                "faultB": 20,
                "title": "신호 위반 측면 충돌 사고",
                "laws": "도로교통법 제5조, 제25조",
                "precedents": "대법원 2020다12345 판결"
            }
        }