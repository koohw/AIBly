from ...models.response_models import AnalysisResponse
from typing import Dict

def generate_report(output: Dict) -> AnalysisResponse:
    analysis_response = AnalysisResponse(
        userId=output.get("userId", 0),
        videoId=output.get("videoId", 0),
        fileName=output.get("fileName", ""),
        accidentPlace=output.get("accidentPlace", ""),
        accidentFeature=output.get("accidentFeature", ""),
        carAProgress=output.get("carAProgress", ""),
        carBProgress=output.get("carBProgress", ""),
        faultA=output.get("faultA", 0),
        faultB=output.get("faultB", 0),
        title=output.get("title", ""),
        laws=output.get("laws", ""),
        precedents=output.get("precedents", "")
    )
    return analysis_response
