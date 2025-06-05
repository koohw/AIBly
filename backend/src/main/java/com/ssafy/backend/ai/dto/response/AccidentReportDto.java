package com.ssafy.backend.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AccidentReportDto {

    private Long userId;
    private Long videoId;
    private String fileName;

    private List<AccidentDefinitionDto> definitions; //AccidentDefinitionDto에 있는 내용들

}

//AI 서버에서 백엔드로 사고 분석 결과 전달 시 사용하는 DTO
//사고 보고서 메타데이터 + 사고 정의 내용 묶음,AccidentDefinitionDto 내용들
