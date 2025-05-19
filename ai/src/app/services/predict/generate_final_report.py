import json
from ...config import Config

def generate_final_report(result_data):
    """
    모든 분석 결과를 종합하여 최종 보고서를 생성합니다.
    
    Args:
        result_data: 이전 단계에서 생성된 분석 결과 데이터
        
    Returns:
        dict: 최종 보고서 데이터
    """
    # 타임라인 데이터 추출
    timeline_data = result_data.get("timeline_analysis", {})
    timeline = timeline_data.get("timeline", {})
    # 이벤트 타임라인 처리
    event_timeline = timeline.get("event_timeline", [])
    
    # 추론된 메타 정보 추출
    inferred_meta_data = result_data.get("inferred_meta", {})
    meta = inferred_meta_data.get("inferred_meta", {})
    
    # 과실 비율 정보 추출
    fault = result_data.get("accident_type", {})
    
    # 최종 보고서 구성
    report = {
        "video": meta.get("video"),
        "location": meta.get("location"),
        "vehicle_A_direction": meta.get("vehicle_A_direction"),
        "vehicle_B_direction": meta.get("vehicle_B_direction"),
        "traffic_light": meta.get("traffic_light"),
        "accident_frame_idx": meta.get("accident_frame_idx"),
        "damage_location": meta.get("damage_location"),
        "accident_type_key": meta.get("accident_type_key"),
        "accident_type_code": fault.get("accident_type_code"),
        "fault_ratio_A": fault.get("fault_ratio_A"),
        "fault_ratio_B": fault.get("fault_ratio_B"),
        "fault_description": fault.get("description"),
        "event_timeline": event_timeline
    }
    
    return {
        "final_report": report
    }