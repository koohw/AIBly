import pandas as pd

def infer_damage_location_from_direction(direction):
    """
    차량 B의 진행 방향을 기반으로 손상 위치를 추론합니다.
    """
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
    base = DIRECTION_DAMAGE_LOOKUP.get(direction, [2])
    if len(base) == 1:
        return str(base[0])
    pair = tuple(sorted(base))
    return ",".join(map(str, pair)) if pair in ALLOWED_COMBINATIONS else str(base[0])

def normalize_dir(raw):
    """방향 이름을 표준화합니다."""
    if raw == "left":      return "from_left"
    if raw == "right":     return "from_right"
    return raw

def infer_vehicleA_direction(csv_path, accident_idx, window=10):
    """
    차량 A의 사고 발생 전 window 프레임 동안의 횡방향 변위를 분석하여 방향 추론
    """
    df = pd.read_csv(csv_path, index_col="frame")
    
    # 분석 대상 프레임 선택
    start = max(0, accident_idx - window)
    
    # 새 코드 방식: ego_pos 기준 (궤적 누적값)
    if "ego_pos" in df.columns:
        lat = df.loc[start:accident_idx, "ego_pos"].astype(float)
    else:
        # 기존 코드 호환성 유지: ensemble_traj 사용
        sub = df.loc[start:accident_idx, "ensemble_traj"].astype(float)
        lat = -sub  # ego lateral = -ensemble_traj
    
    # 분석 구간 순 변위 계산
    disp = lat.iloc[-1] - lat.iloc[0]
    
    # 새 코드 방식: 단순 부호 비교
    if disp > 0:
        return "left"
    elif disp < 0:
        return "right"
    else:
        return "go_straight"

def generate_inferred_meta(result_data):
    """
    이전 단계에서 수집된 데이터를 바탕으로 사고 메타데이터를 추론합니다.
    """
    # video_id 추출
    video_id = result_data.get("videoId", "video_001")
    
    # 타임라인에서 사고 프레임 인덱스 추출
    timeline_data = result_data.get("timeline_analysis", {})
    timeline = timeline_data.get("timeline", {})
    accident_frame_idx = timeline.get("accident_frame_idx", 0)
    
    # 1) 차량 A 방향 추론 (vehicleA_trajectory 결과 사용)
    vehicle_A_direction = "go_straight"  # 기본값
    
    try:
        # vehicleA_trajectory 결과의 csv_path 사용
        vehicleA_data = result_data.get("vehicleA_trajectory", {})
        if vehicleA_data and "csv_path" in vehicleA_data:
            ego_flow_csv = vehicleA_data["csv_path"]
            
            # 차량 A 방향 추론
            raw_A = infer_vehicleA_direction(
                ego_flow_csv,
                accident_frame_idx,
                window=10
            )
            
            # 방향 정규화
            vehicle_A_direction = normalize_dir(raw_A)
        else:
            # vehicleA_trajectory 결과가 없는 경우 기본값 사용
            vehicle_A_direction = "go_straight"
    except Exception:
        # CSV 파일 문제 시 기본값 사용
        vehicle_A_direction = "go_straight"
    
    # 2) 차량 B 방향 로드
    lstm_data = result_data.get("lstm_analysis", {})
    vehicle_B_direction = lstm_data.get("direction", "unknown")
    
    # 방향 정규화
    vehicle_B_direction = normalize_dir(vehicle_B_direction)
    
    # 3) 신호등 정보 로드
    traffic_light_info = result_data.get("traffic_light_info", {})
    traffic_light = traffic_light_info.get("traffic_light", False)
    
    # 4) 손상 위치 로드 또는 추론
    damage_location = None
    vtn_result = result_data.get("vtn_result", {})
    if vtn_result:
        damage_location = vtn_result.get("damage_location")
    
    if not damage_location or str(damage_location).strip().lower() in ["", "none", "null"]:
        damage_location = infer_damage_location_from_direction(vehicle_B_direction)
    
    # 5) 사고 유형 키 생성
    # locationType에 따른 위치 타입 매핑
    LOCATION_TYPE_MAP = {
        1: "straight_road",
        2: "t_junction",
        3: "crossroad",
        4: "roundabout"
    }
    
    # locationType이 있으면 해당 값을 사용, 없으면 기본값 사용
    location_type = result_data.get("locationType", 2)  # 기본값 2 = t_junction
    location = LOCATION_TYPE_MAP.get(location_type, "t_junction")
    
    accident_type_key = f"{location}_{vehicle_A_direction}_{vehicle_B_direction}_{traffic_light}"
    
    # 6) 메타 데이터 구성
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