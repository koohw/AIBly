package com.ssafy.backend.report.dto.response;

import com.ssafy.backend.domain.report.Report;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

//보고서 목록 조회 DTO, 목록에서 필요한 최소 필드만 노출
@Getter
@Setter
@NoArgsConstructor
public class ReportListResponseDto {

    private Long id;
    private String title;
    private String accidentCode;
    private String createdAt;
    private int faultA;
    private int faultB;
    private String fileName;

    //생성자 명확히 표시,@AllArgsConstructor 대신 적음
    public ReportListResponseDto(Long id, String title, String accidentCode, String createdAt, int faultA, int faultB, String fileName) {
        this.id = id;
        this.title = title;
        this.accidentCode = accidentCode;
        this.createdAt = createdAt;
        this.faultA = faultA;
        this.faultB = faultB;
        this.fileName = fileName;
    }

    /**
     * from(Report report)이란? 엔티티(Report)를 받아서 DTO(ReportListResponseDto)로 변환하는 역할
     * 컨트롤러/서비스에서는 깔끔하게 from(report)으로 호출-> new 안해도 됨(아래 예시)
     * ReportListResponseDto dto = new ReportListResponseDto(report.getId(), report.getTitle()...이렇게 했었음
     */
    public static ReportListResponseDto from(Report report) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH시 mm분");

        return new ReportListResponseDto(
                report.getId(),
                report.getTitle(),
                report.getAccidentCode(),
                report.getCreatedAt().format(formatter),
                report.getFaultA(),
                report.getFaultB(),
                report.getFileName()
        );
    }

}