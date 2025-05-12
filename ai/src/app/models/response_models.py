from pydantic import BaseModel, HttpUrl

class AnalysisResponse(BaseModel):
    """AI 분석 응답 모델"""
    userId: int
    videoId: int
    fileName: str
    accidentPlace: str      # 사고 장소
    accidentFeature: str    # 장소 특징
    carAProgress: str       # A 차량 진행 방향
    carBProgress: str       # B 차량 진행 방향
    faultA: int             # A 과실
    faultB: int             # B 과실
    title: str              # 사고 제목
    laws: str               # 법조문
    precedents: str         # 판례례