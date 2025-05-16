import os
from glob import glob
import cv2
import torch
from concurrent.futures import ThreadPoolExecutor
from ultralytics import YOLO
from ...config import Config

def load_image(path):
    img = cv2.imread(path)
    return os.path.basename(path), img

def load_images_parallel(image_paths, max_workers=8):
    """이미지를 병렬로 로드합니다 (CPU 멀티스레딩)"""
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        results = list(executor.map(load_image, image_paths))
    names, images = zip(*[r for r in results if r[1] is not None])
    return list(names), list(images)

def run_yolo_inference(frames_dir: str) -> list:
    """프레임에서 객체 탐지를 수행합니다 (GPU + CPU 병렬 처리)."""

    # 모델 경로 및 디바이스 설정
    model_path = Config.YOLO_MODEL_PATH
    device = "cuda" if torch.cuda.is_available() else "cpu"
    model = YOLO(model_path).to(device)

    # 이미지 파일 경로 정렬
    image_files = sorted(
        glob(os.path.join(frames_dir, "*.png")) + glob(os.path.join(frames_dir, "*.jpg"))
    )

    if not image_files:
        return []

    # 결과 리스트 초기화
    results = []
    
    # 배치 크기 설정 (60개로 제한)
    batch_size = 60
    
    # 이미지를 배치 단위로 처리
    for i in range(0, len(image_files), batch_size):
        batch_files = image_files[i:i+batch_size]
        
        # 이미지 병렬 로딩 (CPU)
        image_names, images = load_images_parallel(batch_files, max_workers=os.cpu_count())
        
        # YOLO 배치 추론 (GPU)
        preds = model(images)
        
        # 결과 정리
        for frame_name, pred in zip(image_names, preds):
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