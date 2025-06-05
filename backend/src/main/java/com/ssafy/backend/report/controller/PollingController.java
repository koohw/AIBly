package com.ssafy.backend.report.controller;

import com.ssafy.backend.domain.file.AnalysisStatus;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.dto.response.ReportStatusResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Report 상태 Polling API (수동 업로드용)
@RestController
@RequestMapping("/api/internal/polling")
@RequiredArgsConstructor
public class PollingController {

    private final ReportRepository reportRepository;

    @GetMapping("/{videoId}/status")
    public ResponseEntity<ReportStatusResponseDto> checkReportStatus(@PathVariable Long videoId) {
        Report report = reportRepository.findByVideoFileId(videoId)
                .orElseThrow(() -> new IllegalArgumentException("보고서가 존재하지 않습니다."));

        // AnalysisStatus 기준으로 응답
        if (report.getAnalysisStatus() == AnalysisStatus.COMPLETED) {
            return ResponseEntity.ok(new ReportStatusResponseDto(report.getAnalysisStatus(), report.getId()));
        } else if (report.getAnalysisStatus() == AnalysisStatus.FAILED) {
            return ResponseEntity.ok(new ReportStatusResponseDto(report.getAnalysisStatus(), null));
        } else {
            // ANALYZING인 경우에도 reportId는 null (아직 없음)
            return ResponseEntity.ok(new ReportStatusResponseDto(report.getAnalysisStatus(), null));
        }
    }
}