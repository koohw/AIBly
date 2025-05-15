from fastapi import APIRouter
from ...models.request_models import AnalysisRequest
from ...models.response_models import AnalysisResponse, TimelineEvent
from ...services.report_service import ReportService
import logging
from fastapi.responses import JSONResponse

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


router = APIRouter(
    prefix="",
    tags=["영상 분석"],
    responses={
        404: {"description": "Not found"},
        500: {"description": "Internal server error"}
    }
)

report_service = ReportService()
@router.post("/analyze", response_model=AnalysisResponse, summary="자동차 사고 영상 분석")
async def analyze_video(request: AnalysisRequest):
    """
    프리사인드 URL로 전달된 자동차 사고 영상을 분석합니다.
    
    영상을 다운로드하고 AI 분석을 통해 사고 정보를 추출합니다.
    
    Parameters:
        request (AnalysisRequest): 사용자 ID, 비디오 ID, 파일명, 프리사인드 URL 정보
        
    Returns:
        AnalysisResponse: 사고 분석 결과를 포함한 응답
    """
    logger.info(f"분석 요청 받음: userId={request.userId}")
    
    response = report_service.analyze_report(request)
    return response


@router.post("/analyze-test", response_model=AnalysisResponse, summary="테스트용 영상 분석")
async def analyze_video_test(request: AnalysisRequest):
    """
    테스트 목적의 자동차 사고 영상 분석 엔드포인트입니다.
    
    실제 분석 없이 테스트용 더미 데이터를 반환합니다.
    
    Parameters:
        request (AnalysisRequest): 사용자 ID, 비디오 ID, 파일명, 프리사인드 URL 정보
        
    Returns:
        AnalysisResponse: 더미 테스트 데이터를 포함한 응답
    """
    logger.info(f"테스트 분석 요청 받음: userId={request.userId}")
    
    try:
        # 테스트용 타임라인 이벤트 생성
        test_timeline = [
            TimelineEvent(event="vehicle_B_entry", frameIdx=0),
            TimelineEvent(event="accident_estimated", frameIdx=30),
            TimelineEvent(event="aftermath", frameIdx=40)
        ]
        
        # 테스트용 더미 데이터 반환
        response_data = AnalysisResponse(
            userId=request.userId,
            videoId=request.videoId,
            fileName=request.fileName,
            carAProgress="go_straight",
            carBProgress="from_right",
            faultA=20,
            faultB=80,
            eventTimeline=test_timeline,
            accidentType=5,
            damageLocation="1,2"
        )
        
        logger.info(f"테스트 응답 반환: {response_data}")
        return response_data
        
    except Exception as e:
        logger.error(f"테스트 중 오류 발생: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"status": "error", "message": f"테스트 처리 중 오류: {str(e)}"}
        )