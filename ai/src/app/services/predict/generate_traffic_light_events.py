import os
import json

# 클래스 매핑을 전역 상수로 정의
CLASS_ID_TO_NAME = {
    2: "traffic-light-red",
    3: "traffic-light-green"
}

def generate_traffic_light_events(yolo_results):
    """
    YOLO 결과에서 신호등 이벤트(적색/녹색)를 추출합니다.
    
    Args:
        yolo_results: YOLO 객체 탐지 결과
        
    Returns:
        list: 신호등 이벤트 정보 리스트
    """
    # 이벤트 수집
    events = []
    for item in yolo_results:
        idx = int(os.path.splitext(item['frame'])[0])
        for box in item.get("boxes", []):
            cls = box.get("class")
            if cls in CLASS_ID_TO_NAME:
                events.append({
                    "frame_idx": idx,
                    "class_name": CLASS_ID_TO_NAME[cls]
                })
    
    # 결과 정렬 (frame_idx 기준)
    events.sort(key=lambda x: x["frame_idx"])
    
    return events