import pandas as pd
from ...config import Config

def generate_final_report(result_data):
    """
    모든 분석 결과를 종합하여 최종 보고서를 생성합니다.
    """
    # 필요한 데이터 추출
    video_id = result_data.get("videoId", "video_001")
    
    # 타임라인 데이터 추출
    timeline_data = result_data.get("timeline_analysis", {})
    timeline = timeline_data.get("timeline", {})
    
    # 사고 유형 추출
    accident_type_data = result_data.get("accident_type", {})
    accident_type = accident_type_data.get("accident_type", "unknown")
    
    # 추론된 메타 정보 추출
    inferred_meta_data = result_data.get("inferred_meta", {})
    inferred_meta = inferred_meta_data.get("inferred_meta", {})
    
    # 손상 위치
    damage_location = accident_type_data.get("damage_location", "1,2")
    
    # 차량 B 방향 정보 (LSTM 결과에서 추출)
    lstm_data = result_data.get("lstm_analysis", {})
    vehicle_B_direction = lstm_data.get("direction", "from_right")
    
    # CSV에서 과실 비율 추출
    csv_path = Config.ACCIDENT_DATA_CSV_PATH
    df = pd.read_csv(csv_path)
    df["사고 유형"] = pd.to_numeric(df["사고 유형"], errors="coerce")
    
    try:
        # 사고 유형에 맞는 행 찾기
        row = df[df["사고 유형"] == accident_type].iloc[0]
        rate_a = int(row["과실 비율 A"])
        rate_b = int(row["과실 비율 B"])
    except:
        # 기본값
        rate_a, rate_b = 0, 100
    
    # 최종 보고서 구성
    final_report = {
        "video_id": video_id,
        "event_timeline": timeline.get("event_timeline", []),
        "accident_type": accident_type,
        "vehicle_A_direction": "go_straight",  # 고정값
        "vehicle_B_direction": vehicle_B_direction,
        "damage_location": damage_location,
        "negligence_rate": {
            "A": rate_a,
            "B": rate_b
        }
    }
    
    return {
        "final_report": final_report
    }