from fastapi import HTTPException
from .report import video_downloader, video_analyzer, report_generator
from ..models.request_models import AnalysisRequest
from ..models.response_models import AnalysisResponse
from .models import AccidentAnalysisModel
import os

class ReportService:
    def __init__(self):
        # 모델 인스턴스 생성
        self.model = AccidentAnalysisModel()

    def analyze_report(self, request: AnalysisRequest) -> AnalysisResponse:
        # 0. 저장 경로
        path = str(request.userId) + "/"
        file_name = str(request.videoId) + ".mp4"
        os.makedirs(path, exist_ok=True)

        # 1. 동영상 다운로드
        try:
            video_downloader.download_report(request.presignedUrl, path, file_name)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"동영상 다운로드 중 오류 발생: {str(e)}")

        # 2. 동영상 분석
        try:
            # 사용자 요청 정보도 함께 전달
            output = video_analyzer.analyze_video(
                path, 
                file_name, 
                self.model,
                request.userId,
                request.videoId,
                request.fileName
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"동영상 분석 중 오류 발생: {str(e)}")

        # 3. 양식에 맞게 결과 생성
        try:
            response = report_generator.generate_report(output)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"결과 생성 중 오류 발생: {str(e)}")

        return response