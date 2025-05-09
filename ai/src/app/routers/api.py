from fastapi import APIRouter
import pkg_resources
import sys
import platform
import requests
import os
from ..models.request_models import AnalysisRequest
import logging
from fastapi.responses import JSONResponse

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 저장 경로 설정
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

# 슬래시로 끝나는 prefix는 사용할 수 없음
# 빈 문자열이나 슬래시로 끝나지 않는 경로를 사용해야 함
router = APIRouter(prefix="")  # 또는 router = APIRouter()

@router.get("/test")
async def test_libraries():
    # 설치된 라이브러리 목록 가져오기
    installed_packages = sorted([f"{pkg.key}=={pkg.version}" 
                              for pkg in pkg_resources.working_set])
    
    # 시스템 정보
    system_info = {
        "python_version": sys.version,
        "platform": platform.platform(),
        "python_path": sys.executable
    }
    
    return {
        "status": "200 OK",
        "message": "라이브러리 테스트 완료",
        "installed_packages": installed_packages,
        "system_info": system_info
    }

@router.post("/analyze")
async def analyze_video(request: AnalysisRequest):
    """
    프리사인드 URL과 사용자 ID를 받아서 영상을 분석하는 엔드포인트
    파일 다운로드 후 완료되면 OK 반환
    """
    logger.info(f"분석 요청 받음: userId={request.userId}")
    
    try:
        # 사용자별 폴더 생성
        user_dir = os.path.join(UPLOAD_DIR, str(request.userId))
        os.makedirs(user_dir, exist_ok=True)
        
        # 파일명 사용
        file_path = os.path.join(user_dir, request.fileName)
        
        # 프리사인드 URL에서 파일 다운로드
        logger.info(f"파일 다운로드 시작: {request.presignedUrl}")
        response = requests.get(request.presignedUrl, stream=True)
        
        if response.status_code != 200:
            logger.error(f"파일 다운로드 실패: {response.status_code}")
            return JSONResponse(
                status_code=400,
                content={"status": "error", "message": f"파일 다운로드 실패: {response.status_code}"}
            )
            
        # 파일 저장
        with open(file_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        
        logger.info(f"파일 다운로드 완료: {file_path}")
        
        # 실제 AI 모델 처리 로직은 차후 구현
        # result = process_with_ai_model(file_path)
        
        # 요청대로 OK 응답 반환
        return {
            "status": "OK"
        }
        
    except Exception as e:
        logger.error(f"분석 중 오류 발생: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"status": "error", "message": f"분석 처리 중 오류: {str(e)}"}
        )

@router.post("/analyze-test")
async def analyze_video_test(request: AnalysisRequest):
    """
    테스트용 엔드포인트 - 다운로드 없이 받은 JSON을 그대로 반환
    """
    logger.info(f"테스트 분석 요청 받음: userId={request.userId}")
    
    try:
        # 다운로드 작업 없이 받은 JSON 데이터를 그대로 반환
        response_data = {
            "received_data": {
                "userId": request.userId,
                "videoId": request.videoId,
                "fileName": request.fileName,
                "presignedUrl": str(request.presignedUrl)
            }
        }
        
        logger.info(f"테스트 응답 반환: {response_data}")
        return response_data
        
    except Exception as e:
        logger.error(f"테스트 중 오류 발생: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"status": "error", "message": f"테스트 처리 중 오류: {str(e)}"}
        )