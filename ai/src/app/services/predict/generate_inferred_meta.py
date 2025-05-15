def generate_inferred_meta(result_data):
    """
    이전 단계에서 수집된 데이터를 바탕으로 사고 메타데이터를 추론합니다.
    """
    # 필요한 데이터 추출
    video_id = result_data.get("videoId", "video_001")
    
    # LSTM 결과에서 방향 정보 추출
    lstm_data = result_data.get("lstm_analysis", {})
    direction = lstm_data.get("direction", "unknown")
    
    # 타임라인 데이터에서 사고 프레임 정보 추출
    timeline_data = result_data.get("timeline_analysis", {})
    timeline = timeline_data.get("timeline", {})
    accident_frame = timeline_data.get("accident_frame", {})
    accident_frame_idx = timeline.get("accident_frame_idx", 0)
    
    # 신호등 정보 추출
    traffic_light_info = result_data.get("traffic_light_info", {})
    traffic_light = traffic_light_info.get("traffic_light", False)
    
    # A 방향과 사고 장소 고정
    vehicle_A_direction = "go_straight"
    location = "t_junction"
    
    # 차량 B 방향
    vehicle_B_direction = direction
    
    # VTN 결과에서 damage_location 추출 시도
    vtn_result = result_data.get("vtn_result", {})
    damage_location = vtn_result.get("damage_location", None)
    
    # 인접 조합만 허용되는 룰 기반 추론 함수
    ALLOWED_COMBINATIONS = {
        (1, 2), (1, 4),
        (2, 3), (3, 5),
        (4, 6), (5, 8),
        (6, 7), (7, 8)
    }

    DIRECTION_DAMAGE_LOOKUP = {
        "from_right": [1, 2],
        "from_left": [2, 3],
        "center": [2],
        "unknown": [7]
    }
    
    # 손상 위치 추론 함수
    def infer_damage_location_from_direction(direction):
        base = DIRECTION_DAMAGE_LOOKUP.get(direction, [2])
        if len(base) == 1:
            return str(base[0])
        pair = tuple(sorted(base))
        if pair in ALLOWED_COMBINATIONS:
            return ",".join(map(str, pair))
        else:
            return str(base[0])
    
    # 비어있거나 누락된 경우 추론
    if not damage_location or str(damage_location).strip().lower() in ["", "none", "null"]:
        damage_location = infer_damage_location_from_direction(vehicle_B_direction)
    
    # 사고유형 키 생성
    accident_type_key = f"{location}_{vehicle_A_direction}_{vehicle_B_direction}_{traffic_light}"
    
    # 메타 통합
    meta = {
        "video": video_id,
        "location": location,
        "vehicle_A_direction": vehicle_A_direction,
        "vehicle_B_direction": vehicle_B_direction,
        "accident_frame_idx": accident_frame_idx,
        "traffic_light": traffic_light,
        "damage_location": damage_location,
        "accident_type_key": accident_type_key
    }
    
    return {
        "inferred_meta": meta
    }