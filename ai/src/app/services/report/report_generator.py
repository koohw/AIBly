from ...models.response_models import AnalysisResponse, TimelineEvent
from typing import Dict

def generate_report(output: Dict) -> AnalysisResponse:
    """
    분석 결과에서 API 응답 모델을 생성합니다.
    
    Args:
        output: 전체 분석 결과 데이터
        
    Returns:
        AnalysisResponse: API 응답 모델
    """
    final_report_data = output.get("final_report", {})
    final_report = final_report_data.get("final_report", {})
    
    # 타임라인 이벤트 변환
    timeline_events = []
    for event_data in final_report.get("event_timeline", []):
        timeline_events.append(TimelineEvent(
            event=event_data.get("event", ""),
            frameIdx=event_data.get("frame_idx", 0)
        ))
    
    # 과실 비율 처리
    fault_ratio_a = final_report.get("fault_ratio_A")
    fault_ratio_b = final_report.get("fault_ratio_B")
    
    # None인 경우 기본값 설정
    if fault_ratio_a is None or fault_ratio_b is None:
        fault_ratio_a = 0
        fault_ratio_b = 100
    
    # accident_type_code 숫자 변환 확인
    accident_type = final_report.get("accident_type_code", 0)
    try:
        accident_type = int(accident_type)
    except (ValueError, TypeError):
        accident_type = 0
    
    # 응답 생성
    response = AnalysisResponse(
        userId=output.get("userId", 0),
        videoId=int(output.get("videoId", 0)),
        fileName=output.get("fileName", ""),
        carAProgress=final_report.get("vehicle_A_direction", ""),
        carBProgress=final_report.get("vehicle_B_direction", ""),
        faultA=fault_ratio_a,
        faultB=fault_ratio_b,
        eventTimeline=timeline_events,
        accidentType=accident_type,
        damageLocation=final_report.get("damage_location", "")
    )
    
    return response