package com.ssafy.backend.report.service;


import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;

import java.util.List;

public interface ReportService {
    List<ReportListResponseDto> getMyReports(Long userId); //마이페이지 보고서 목록

    ReportDetailResponseDto getMyReportDetail(Long userId, Long reportId); //보고서 상세 조회

    void deleteMyReport(Long userId, Long reportId); //보고서 삭제
}