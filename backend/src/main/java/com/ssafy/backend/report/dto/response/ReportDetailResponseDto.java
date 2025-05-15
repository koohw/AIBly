package com.ssafy.backend.report.dto.response;

import com.ssafy.backend.domain.report.Report;
import lombok.*;

import java.time.LocalDateTime;

//보고서 상세 조회
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailResponseDto {

    private Long id;
    private String title;
    private String accidentType;
    private String laws;
    private String precedents;
    private String carAProgress;
    private String carBProgress;
    private int faultA;
    private int faultB;
    private String damageLocation;
    private String eventTimeline;
    private LocalDateTime createdAt;


    //Report Entity를 ReportDetailResponseDto로 변환해주는 정적 메서드
    public static ReportDetailResponseDto from(Report report) {
        return ReportDetailResponseDto.builder()
                .id(report.getId())
                .title(report.getTitle())
                .accidentType(report.getAccidentType())
                .laws(report.getLaws())
                .precedents(report.getDecisions())
                .carAProgress(report.getCarA())
                .carBProgress(report.getCarB())
                .faultA(report.getFaultA())
                .faultB(report.getFaultB())
                .damageLocation(report.getDamageLocation())
                .eventTimeline(report.getMainEvidence())
                .createdAt(report.getCreatedAt())
                .build();
    }
}