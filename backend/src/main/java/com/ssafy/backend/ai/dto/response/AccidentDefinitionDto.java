package com.ssafy.backend.ai.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccidentDefinitionDto {

    private String accidentPlace;      // 사고 장소
    private String accidentFeature;    // 장소 특징
    private String carAProgress;       // A 차량 진행 방향
    private String carBProgress;       // B 차량 진행 방향
    private int faultA;                // A 과실
    private int faultB;                // B 과실
    private String title;              // 사고 제목
    private String laws;               // 법조문
    private String precedents;         // 판례
}

//CSV의 한 줄을 표현하는 단순한 데이터 구조 (DB와 무관)
//AI 서버가 생성한 CSV의 각 Row를 담기 위한 구조
//AI → 백엔드 → 사용자에게 전달될 '정적 데이터 구조'