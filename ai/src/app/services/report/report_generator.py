from ...models.response_models import AnalysisResponse, TimelineEvent
from typing import Dict

def generate_report(output: Dict) -> AnalysisResponse:
    final_report_data = output.get("final_report", {})
    final_report = final_report_data.get("final_report", {})
    
    # 타임라인 이벤트 변환
    timeline_events = []
    for event_data in final_report.get("event_timeline", []):
        timeline_events.append(TimelineEvent(
            event=event_data.get("event", ""),
            frameIdx=event_data.get("frame_idx", 0)
        ))
    
    # 과실 비율
    negligence_rate = final_report.get("negligence_rate", {"A": 0, "B": 100})
    
    # accident_type 숫자 변환 확인
    accident_type = final_report.get("accident_type", 0)
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
        faultA=negligence_rate.get("A", 0),
        faultB=negligence_rate.get("B", 100),
        eventTimeline=timeline_events,
        accidentType=accident_type,
        damageLocation=final_report.get("damage_location", "")
    )
    
    return response