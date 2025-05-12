package com.ssafy.backend.report.dto.response;

import com.ssafy.backend.domain.report.Report;
import lombok.AllArgsConstructor;
import lombok.Getter;

//보고서 목록 조회 DTO, 목록에서 필요한 최소 필드만 노출
@Getter
@AllArgsConstructor
public class ReportListResponseDto {

    private Long id;
    private String title;
    private String accidentCode;
    private String createdAt;

    /**
     * from(Report report)이란? 엔티티(Report)를 받아서 DTO(ReportListResponseDto)로 변환하는 역할
     * 컨트롤러/서비스에서는 깔끔하게 from(report)으로 호출-> new 안해도 됨(아래 예시)
     * ReportListResponseDto dto = new ReportListResponseDto(report.getId(), report.getTitle()...이렇게 했었음
     */
    public static ReportListResponseDto from(Report report) {
        return new ReportListResponseDto(
                report.getId(),
                report.getTitle(),
                report.getAccidentCode(),
                report.getCreatedAt().toString()
        );
    }
}