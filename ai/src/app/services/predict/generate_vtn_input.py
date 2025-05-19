import numpy as np
import os
import pickle
import json
import logging
from ...config import Config

logger = logging.getLogger(__name__)

def generate_vtn_input(result_data):
    """
    YOLO 결과와 LSTM 방향 추론 결과를 바탕으로 VTN 모델의 입력을 생성하고 pickle 파일로 저장합니다.
    
    Args:
        result_data: 이전 단계에서 생성된 분석 결과 데이터
        
    Returns:
        dict: VTN 입력 파일 경로 정보
    """
    # 1. 필요한 데이터 추출
    yolo_data = result_data.get("yolo_analysis", [])
    
    # LSTM 결과에서 방향 정보 추출
    lstm_data = result_data.get("lstm_analysis", {})
    direction = lstm_data.get("direction", "unknown")
    
    # 타임라인 데이터에서 사고 프레임 정보 추출
    timeline_data = result_data.get("timeline_analysis", {})
    accident_frame = timeline_data.get("accident_frame", {})
    accident_frame_idx = accident_frame.get("frame_idx", 0)
    
    video_id = result_data.get("videoId", "unknown")
    
    # 2. vehicle_B bbox 시퀀스 추출
    bbox_sequence = []
    for item in yolo_data:
        frame_idx = int(os.path.splitext(item["frame"])[0])
        for box in item.get("boxes", []):
            if box.get("class") == 1:  # vehicle_B class
                x1, y1, x2, y2 = box["bbox"]
                bbox_sequence.append({
                    "frame_idx": frame_idx,
                    "bbox": [x1, y1, x2, y2]
                })
                break  # 한 프레임당 하나만

    # 3. 정렬 및 벡터화
    bbox_sequence.sort(key=lambda x: x["frame_idx"])
    bbox_array = np.array([b["bbox"] for b in bbox_sequence], dtype=np.float32)
    attention_mask = np.ones(len(bbox_array), dtype=np.int32)
    
    # 4. category_tensor 구성 (direction 기반 one-hot)
    direction_map = {"from_left": 0, "center": 1, "from_right": 2}
    category_tensor = np.zeros(3, dtype=np.float32)
    if direction in direction_map:
        category_tensor[direction_map[direction]] = 1.0
    
    # 디버그 로깅
    logger.debug(f"Loaded direction: {direction!r}")
    logger.debug(f"category_tensor: {category_tensor.tolist()}, sum={category_tensor.sum()}")
    
    # 5. VTN 입력 dict 구성
    vtn_input = {
        "video_id": video_id,
        "category_tensor": category_tensor,
        "bbox_sequence": bbox_array,
        "attention_mask": attention_mask,
        "accident_frame": accident_frame_idx
    }
    
    # 6. 저장 경로 설정 및 저장
    vtn_pkl_path = os.path.join(Config.VTN_PKL_PATH, f"{video_id}_vtn_input.pkl")
    os.makedirs(os.path.dirname(vtn_pkl_path), exist_ok=True)
    
    with open(vtn_pkl_path, 'wb') as f:
        pickle.dump(vtn_input, f)
    
    logger.info(f"VTN 입력 저장 완료: {vtn_pkl_path}")
    logger.info(f"bbox 시퀀스 길이: {len(bbox_array)}")
    logger.info(f"방향: {direction} → {category_tensor.tolist()}")
    
    # 단순화된 구조로 반환
    return {
        "file_path": vtn_pkl_path,
        "video_id": video_id,
        "sequence_length": len(bbox_array),
        "direction": direction
    }