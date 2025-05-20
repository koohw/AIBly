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

def infer_vehicleA_direction(csv_path, accident_idx, window=10, threshold=5):
    """
    차량 A의 사고 발생 전 window 프레임 동안의 횡방향 변위를 분석하여 방향 추론
    """
    df = pd.read_csv(csv_path, index_col="frame")
    # 분석 대상 프레임 선택
    start = max(0, accident_idx - window)
    sub = df.loc[start:accident_idx, "ensemble_traj"].astype(float)
    # ego lateral = -ensemble_traj
    ego_lat = -sub
    # 분석 구간 순 변위
    disp = ego_lat.iloc[-1] - ego_lat.iloc[0]
    if disp > threshold:
        return "move_left"
    if disp < -threshold:
        return "move_right"
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
            vehicle_A_direction = infer_vehicleA_direction(
                ego_flow_csv,
                accident_frame_idx,
                window=10,
                threshold=5
            )
        else:
            # vehicleA_trajectory 결과가 없는 경우 기본값 사용
            vehicle_A_direction = "go_straight"
    except Exception:
        # CSV 파일 문제 시 기본값 사용
        vehicle_A_direction = "go_straight"
    
    # 2) 차량 B 방향 로드
    lstm_data = result_data.get("lstm_analysis", {})
    vehicle_B_direction = lstm_data.get("direction", "unknown")
    
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
    location = "t_junction"
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