import json
from ...config import Config
import logging

logger = logging.getLogger(__name__)    

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
    
    # 새 코드 방식으로 타임라인 처리 
    # 타임라인 데이터가 다양한 구조로 제공될 수 있음
    timeline = timeline_data.get("timeline", timeline_data)
    
    # 일부 타임라인 파일은 event_timeline 아래에 이벤트를 중첩시킴
    # 없으면 timeline 자체를 이벤트 리스트로 간주
    event_timeline = timeline.get("event_timeline", timeline)
    
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
    logger.info(f"최종 보고서 생성 완료: {report}")
    return {
        "final_report": report
    }