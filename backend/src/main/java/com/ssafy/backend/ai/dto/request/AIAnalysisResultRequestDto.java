package com.ssafy.backend.ai.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AIAnalysisResultRequestDto {

    private Prediction prediction;
    private List<TimelineLog> timeline_log;

    @Getter
    @Setter
    public static class Prediction {
        private int accident_type;
        private List<Integer> damage_location;
        private int accident_place;
        private int accident_place_feature;
        private int vehicle_a_progress_info;
        private int vehicle_b_progress_info;
        private int fault_a;
        private int fault_b;
    }

    @Getter
    @Setter
    public static class TimelineLog {
        private String time;
        private List<String> events;
    }
}
//AI 서버에서 보내주는 데이터니까 Response DTO는 필요 없음