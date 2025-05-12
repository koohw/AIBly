import os
from typing import Dict, Any
from .services.preprocessor import preprocess_video
from .services.analyzer import analyze_accident
import logging

MODEL_NAME = "dummy.model"

class AccidentAnalysisModel:
    def __init__(self):
        """사고 분석 모델 초기화"""
        self.model_name = MODEL_NAME
        self.model_path = os.path.join(os.path.dirname(__file__), MODEL_NAME)
        logging.info(f"모델 경로: {self.model_path}")
        
        self.model = MODEL_NAME
        
    def analyze(self, video_path: str) -> Dict[str, Any]:
        """비디오 분석 실행"""
        # 1. 비디오 전처리
        processed_data = preprocess_video(video_path)
        
        # 2. 분석 실행
        results = analyze_accident(self.model, processed_data)
        
        return results

# 모델 인스턴스 가져오는 함수
def get_model():
    return AccidentAnalysisModel()