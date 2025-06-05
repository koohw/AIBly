package com.ssafy.backend.report.service;


import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;

import java.util.List;

public interface ReportService {
    List<ReportListResponseDto> getMyReports(Long userId); //마이페이지 보고서 목록

    ReportDetailResponseDto getMyReportDetail(Long userId, Long reportId); //보고서 상세 조회

    void deleteMyReport(Long userId, Long reportId, boolean alsoDeleteVideo); //보고서 삭제

    //내 영상함에서 보고서 재생성
    void recreateReport(Long videoId, Long userId);

    //pdf 여러번 다운
    String getPdfPresignedUrl(Long userId, Long reportId);
}