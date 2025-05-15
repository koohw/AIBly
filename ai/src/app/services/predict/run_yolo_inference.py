import os
from ultralytics import YOLO
from glob import glob
import cv2
from ...config import Config

def run_yolo_inference(frames_dir: str) -> list:
    """프레임에서 객체 탐지를 수행합니다."""
    # 모델 경로
    model_path = Config.YOLO_MODEL_PATH
    
    # YOLO 모델 로드
    model = YOLO(model_path)
    
    # 결과 저장용 리스트
    results = []
    
    # 프레임 순서대로 정렬
    image_files = sorted(glob(os.path.join(frames_dir, "*.png")) + glob(os.path.join(frames_dir, "*.jpg")))
    
    # 프레임별 YOLO 추론
    for frame_path in image_files:
        frame_name = os.path.basename(frame_path)
        frame = cv2.imread(frame_path)
        
        pred = model(frame)[0]
        frame_result = {
            "frame": frame_name,
            "boxes": []
        }
        
        for box in pred.boxes.data.tolist():
            x1, y1, x2, y2, score, cls = box
            frame_result["boxes"].append({
                "bbox": [x1, y1, x2, y2],
                "score": score,
                "class": int(cls)
            })
            
        results.append(frame_result)
    
    return results