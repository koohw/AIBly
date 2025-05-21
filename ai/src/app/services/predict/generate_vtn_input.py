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
    direction_B = lstm_data.get("direction", "unknown")
    
    # B 차량 방향 정규화
    if direction_B == "left":
        direction_B = "from_left"
    elif direction_B == "right":
        direction_B = "from_right"
    elif direction_B == "straight":
        direction_B = "center"
    
    # A 차량 방향은 항상 "center"로 고정
    direction_A = "center"
    
    # 타임라인 데이터에서 사고 프레임 정보 추출
    timeline_data = result_data.get("timeline_analysis", {})
    accident_frame = timeline_data.get("accident_frame", {})
    accident_frame_idx = accident_frame.get("frame_idx", 0)
    
    video_id = result_data.get("videoId", "unknown")
    
    # 2. vehicle_B bbox 시퀀스 추출
    bbox_sequence = []
    for item in yolo_data:
        for box in item.get("boxes", []):
            if box.get("class") == 1:  # vehicle_B class
                x1, y1, x2, y2 = box["bbox"]
                bbox_sequence.append([x1, y1, x2, y2])
                break  # 한 프레임당 하나만

    # 3. 벡터화
    bbox_array = np.array(bbox_sequence, dtype=np.float32)
    attention_mask = np.ones(len(bbox_array), dtype=np.int32)
    
    # 4. category_tensor 구성 (A차량 + B차량 각각의 방향 기반 one-hot, 총 6차원)
    direction_map = {"from_left": 0, "center": 1, "from_right": 2}
    # 6차원 (A차량 3차원 + B차량 3차원)
    category_tensor = np.zeros(6, dtype=np.float32)
    
    # A차량 방향 설정
    if direction_A in direction_map:
        category_tensor[direction_map[direction_A]] = 1.0
        
    # B차량 방향 설정 (인덱스 3,4,5)
    if direction_B in direction_map:
        category_tensor[3 + direction_map[direction_B]] = 1.0
    
    # 디버그 로깅
    logger.info(f"A차량 방향: {direction_A!r}, B차량 방향: {direction_B!r}")
    logger.info(f"category_tensor: {category_tensor.tolist()}")
    logger.info(f"bbox_sequence: {len(bbox_sequence)}")
    logger.info(f"bbox_array: {bbox_array.shape}")
    logger.info(f"attention_mask: {attention_mask.shape}")
    logger.info(f"accident_frame_idx: {accident_frame_idx}")
    
    # 5. VTN 입력 dict 구성
    vtn_input = {
        "video_id": video_id,
        "category_tensor": category_tensor,
        "bbox_sequence": bbox_array,
        "attention_mask": attention_mask,
        "accident_frame": accident_frame_idx,
        "b_direction_text": direction_B
    }
    
    # 6. 저장 경로 설정 및 저장
    vtn_pkl_path = os.path.join(Config.VTN_PKL_PATH, f"{video_id}_vtn_input.pkl")
    os.makedirs(os.path.dirname(vtn_pkl_path), exist_ok=True)
    
    with open(vtn_pkl_path, 'wb') as f:
        pickle.dump(vtn_input, f)
    
    logger.info(f"✅ VTN input 저장 완료: {vtn_pkl_path} (frames: {len(bbox_array)})")
    
    # 단순화된 구조로 반환
    return {
        "file_path": vtn_pkl_path,
        "video_id": video_id,
        "sequence_length": len(bbox_array),
        "direction_A": direction_A,
        "direction_B": direction_B
    }