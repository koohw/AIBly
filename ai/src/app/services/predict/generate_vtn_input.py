import numpy as np
import os
import pickle
import logging
from ...config import Config


def generate_vtn_input(result_data):
    """
    YOLO 결과와 LSTM 방향 추론 결과를 바탕으로 VTN 모델의 입력을 생성하고 pickle 파일로 저장합니다.
    """
    # 필요한 데이터 추출
    yolo_results = result_data.get("yolo_analysis", [])
    
    # LSTM 결과에서 방향 정보 추출
    lstm_data = result_data.get("lstm_analysis", {})
    direction = lstm_data.get("direction", "unknown")
    
    # 타임라인 데이터에서 사고 프레임 정보 추출
    timeline_data = result_data.get("timeline_analysis", {})
    timeline = timeline_data.get("timeline", {})
    accident_frame_idx = timeline.get("accident_frame_idx", 0)
    
    video_id = result_data.get("videoId", "unknown")  # 이전 단계에서 이어지는 값 사용
    
    # vehicle_B bbox 시퀀스 추출
    bbox_sequence = []
    for item in yolo_results:
        frame_idx = int(os.path.splitext(item["frame"])[0])
        for box in item.get("boxes", []):
            if box["class"] == 1:  # vehicle_B class
                x1, y1, x2, y2 = box["bbox"]
                bbox = [x1, y1, x2, y2]
                bbox_sequence.append({
                    "frame_idx": frame_idx,
                    "bbox": bbox
                })
                break  # 한 프레임에 한 개만 추출

    # 정렬 및 벡터화
    bbox_sequence.sort(key=lambda x: x["frame_idx"])
    bbox_array = np.array([b["bbox"] for b in bbox_sequence], dtype=np.float32)
    attention_mask = np.ones(len(bbox_array), dtype=np.int32)
    
    # category_tensor 구성 (direction 기반 one-hot)
    direction_map = {
        "from_left": 0,
        "center": 1,
        "from_right": 2
    }
    category_tensor = np.zeros(3, dtype=np.float32)
    if direction in direction_map:
        category_tensor[direction_map[direction]] = 1.0
    
    # VTN 입력 dict 구성
    vtn_input = {
        "video_id": video_id,
        "category_tensor": category_tensor,
        "bbox_sequence": bbox_array,
        "attention_mask": attention_mask,
        "accident_frame": accident_frame_idx
    }
    
    # 파일 경로 설정
    vtn_pkl_path = os.path.join(Config.VTN_PKL_PATH, f"{video_id}_vtn_input.pkl")
    
    # Pickle 파일로 저장
    with open(vtn_pkl_path, 'wb') as f:
        pickle.dump(vtn_input, f)
    
    
    # 단순화된 구조로 반환
    return {
        "file_path": vtn_pkl_path,
        "video_id": video_id
    }