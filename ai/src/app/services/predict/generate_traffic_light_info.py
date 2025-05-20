def generate_traffic_light_info(traffic_light_events_data):
    """
    신호등 이벤트 데이터와 사고 프레임 정보를 기반으로 신호등 감지 정보를 생성합니다.
    
    Args:
        traffic_light_events_data: 이전 단계에서 생성된 신호등 이벤트 데이터
        
    Returns:
        dict: 신호등 감지 정보
    """
    # 입력 데이터 추출
    yolo_data = traffic_light_events_data.get("yolo_results", [])
    
    # 타임라인에서 사고 프레임 인덱스 가져오기
    accident_frame_idx = traffic_light_events_data.get("accident_frame_idx", 0)
    
    # YOLO 클래스 번호: 2 = traffic-light-red, 3 = traffic-light-green
    traffic_light_classes = [2, 3]
    
    # 사고 이전 프레임에서 신호등 감지 여부 확인
    traffic_light_visible = False
    first_seen_idx = None
    
    for item in yolo_data:
        frame_idx = int(item.get("frame", "0").split(".")[0])
        
        # 사고 이후 프레임은 무시
        if frame_idx >= accident_frame_idx:
            break
            
        for box in item.get("boxes", []):
            class_id = box.get("class")
            if class_id in traffic_light_classes:
                traffic_light_visible = True
                first_seen_idx = frame_idx
                break
                
        if traffic_light_visible:
            break
    
    # 결과 생성
    result = {
        "traffic_light": traffic_light_visible,
        "first_seen_before_accident": first_seen_idx
    }
    
    return result