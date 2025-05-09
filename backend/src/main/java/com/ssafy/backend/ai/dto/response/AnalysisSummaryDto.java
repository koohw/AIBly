package com.ssafy.backend.ai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AnalysisSummaryDto {
    private int accidentType; //사고 유형
    private List<Integer> damageLocation; //파손 위치
    private int accidentPlace; //사고 발생 장소
    private int accidentPlaceFeature;// 사고의 환경요소
    private int vehicleAProgressInfo; //차량 a의 주행상태
    private int vehicleBProgressInfo; //차량 b의 주행상태
}

// 무슨 사건인지 대략적으로 알려줌.