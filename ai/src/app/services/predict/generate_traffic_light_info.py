def generate_traffic_light_info(traffic_light_events_data):
    """
    신호등 이벤트 데이터와 사고 프레임 정보를 기반으로 신호등 감지 정보를 생성합니다.
    
    Args:
        traffic_light_events_data: 이전 단계에서 생성된 신호등 이벤트 데이터
        
    Returns:
        dict: 신호등 감지 정보
    """
    # 입력 데이터 추출
    traffic_light_events = traffic_light_events_data.get("traffic_light_events", [])
    
    # 타임라인에서 사고 프레임 인덱스 가져오기
    accident_frame_idx = traffic_light_events_data.get("accident_frame_idx", 0)
    
    # YOLO 클래스 번호 기준: 2 = traffic-light-red, 3 = traffic-light-green
    TRAFFIC_LIGHT_CLASSES = {2, 3}
    
    # 사고 이전 프레임에서 신호등 감지 여부 확인
    traffic_light_visible = False
    first_seen_idx = None
    
    for event in traffic_light_events:
        frame_idx = event.get("frame_idx", 0)
        
        # 사고 이후 프레임은 무시
        if frame_idx >= accident_frame_idx:
            continue
        
        # 클래스 아이디 추출
        class_name = event.get("class_name", "")
        class_id = None
        
        if class_name == "traffic-light-red":
            class_id = 2
        elif class_name == "traffic-light-green": 
            class_id = 3
        
        # 신호등 클래스 검사
        if class_id in TRAFFIC_LIGHT_CLASSES:
            traffic_light_visible = True
            first_seen_idx = frame_idx
            break
    
    # 결과 생성
    result = {
        "traffic_light": traffic_light_visible,
        "first_seen_before_accident": first_seen_idx
    }
    
    return result