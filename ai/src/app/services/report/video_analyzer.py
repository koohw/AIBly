from fastapi import HTTPException
from typing import Dict
import os
from ..models import AccidentAnalysisModel

def analyze_video(path: str, file_name: str, model: AccidentAnalysisModel, 
                 user_id: int = 0, video_id: int = 0, file_name_orig: str = "") -> Dict:
    # 1. 동영상 분석
    try:
        # 동영상 파일 경로 생성
        video_path = os.path.join(path, file_name)
        
        # 동영상 분석 실행
        result = model.analyze(video_path)
        
        # 사용자 요청 정보 추가
        result["userId"] = user_id
        result["videoId"] = video_id
        result["fileName"] = file_name_orig
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"동영상 분석 중 오류 발생: {str(e)}")
    
    # 2. 동영상 분석 결과 반환
    return result