import os

def generate_timeline_log(yolo_results, video_id, direction=None):
    """
    YOLO 결과를 분석하여 사고 시점을 추론하고 타임라인을 생성합니다.
    
    Args:
        yolo_results: YOLO 객체 탐지 결과
        video_id: 비디오 ID
        direction: B 차량의 진행 방향 정보 (선택적)
    
    Returns:
        사고 프레임 및 타임라인 정보
    """
    # vehicle_B 클래스 번호
    VEHICLE_B_CLASS = 1
    
    # 사고 시점 = vehicle_B의 bbox 크기(w*h)가 최소인 frame
    min_area = float("inf")
    accident_frame = None
    
    for item in yolo_results:
        frame = item["frame"]
        frame_idx = int(os.path.splitext(frame)[0])
        
        for box in item.get("boxes", []):
            if box["class"] == VEHICLE_B_CLASS:
                x1, y1, x2, y2 = box["bbox"]
                w = x2 - x1
                h = y2 - y1
                area = w * h
                
                if area < min_area:
                    min_area = area
                    accident_frame = {
                        "frame": frame,
                        "frame_idx": frame_idx,
                        "area": area
                    }
    
    # 예외 처리: B 차량이 감지되지 않은 경우
    if accident_frame is None:
        accident_frame = {
            "frame": "00000.jpg",
            "frame_idx": 0,
            "area": 0
        }
    
    # 타임라인 구성
    timeline = {
        "video": str(video_id),
        "accident_frame": accident_frame["frame"],
        "accident_frame_idx": accident_frame["frame_idx"],
        "event_timeline": [
            {"event": "vehicle_B_entry", "frame_idx": 0},
            {"event": "accident_estimated", "frame_idx": accident_frame["frame_idx"]},
            {"event": "aftermath", "frame_idx": accident_frame["frame_idx"] + 10}
        ]
    }
    
    # 방향 정보가 제공된 경우 타임라인에 추가
    if direction:
        timeline["vehicle_B_direction"] = direction
    
    return {
        "accident_frame": accident_frame,
        "timeline": timeline
    }