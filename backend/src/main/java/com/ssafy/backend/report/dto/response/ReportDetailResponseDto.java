package com.ssafy.backend.report.dto.response;

import com.ssafy.backend.domain.report.Report;
import lombok.AllArgsConstructor;
import lombok.Getter;

//보고서 상세 조회
@Getter
@AllArgsConstructor
public class ReportDetailResponseDto {

    private Long id;
    private String title;
    private String accidentType;
    private String carA;
    private String carB;
    private String mainEvidence;
    private String laws;
    private String decisions;
    private String createdAt;
    private String pdfKey;


    public static ReportDetailResponseDto from(Report report) {
        return new ReportDetailResponseDto(
                report.getId(),
                report.getTitle(),
                report.getAccidentType(),
                report.getCarA(),
                report.getCarB(),
                report.getMainEvidence(),
                report.getLaws(),
                report.getDecisions(),
                report.getCreatedAt().toString(),
                report.getPdfKey()
        );
    }
}