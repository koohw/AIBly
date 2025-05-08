package com.ssafy.backend.ai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiCallbackRequestDto {
    private AnalysisSummaryDto analysisSummary;
    private List<TimelineLogDto> timelineLog;

}

//ai 분석 후 결과를 백엔드에 전달(json)