import os

def generate_traffic_light_events(yolo_results):
    """
    YOLO 결과에서 신호등 이벤트(적색/녹색)를 추출합니다.
    
    Args:
        yolo_results: YOLO 객체 탐지 결과
        
    Returns:
        dict: 신호등 이벤트 정보를 담은 딕셔너리
    """
    # 클래스 매핑
    CLASS_ID_TO_NAME = {
        2: "traffic-light-red",
        3: "traffic-light-green"
    }
    TARGET_CLASSES = set(CLASS_ID_TO_NAME.keys())
    
    # 이벤트 수집
    events = []
    for item in yolo_results:
        frame = item["frame"]
        frame_idx = int(os.path.splitext(frame)[0])
        
        for box in item.get("boxes", []):
            cls = box["class"]
            if cls in TARGET_CLASSES:
                events.append({
                    "frame_idx": frame_idx,
                    "class_name": CLASS_ID_TO_NAME[cls]
                })
    
    # 결과 정렬 (frame_idx 기준)
    events.sort(key=lambda x: x["frame_idx"])
    
    return {
        "traffic_light_events": events
    }