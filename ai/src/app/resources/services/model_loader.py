import os
import logging

def load_model(model_path=None):
    """모델 로드 함수"""
    if model_path is None:
        model_path = os.path.join(os.path.dirname(__file__), 'data/accident_model.pt')
    
    logging.info(f"모델 로드 중: {model_path}")
    
    # TODO: 실제 모델 로딩 코드 구현
    # from torch import load
    # return load(model_path)
    
    return None  # 임시 반환값