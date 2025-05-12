package com.ssafy.backend.report.controller;

import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.dto.request.ReportPdfDownloadUrlRequestDto;
import com.ssafy.backend.report.dto.request.ReportPdfRequestDto;
import com.ssafy.backend.report.dto.response.ReportPdfDownloadUrlResponseDto;
import com.ssafy.backend.report.dto.response.ReportPdfResponseDto;
import com.ssafy.backend.report.service.PdfService;
import com.ssafy.backend.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfService pdfService;
    private  final ReportRepository reportRepository;
    private  final S3UploadService s3UploadService;

    @PostMapping("/save-pdf")
    public ResponseEntity<ReportPdfResponseDto> savePdf(@RequestBody ReportPdfRequestDto requestDto) {
        String s3Key = pdfService.generateAndUploadPdf(requestDto.getReportId());
        return ResponseEntity.ok(new ReportPdfResponseDto("PDF 저장 완료", s3Key));
    }

    //PDF 다운로드용 Presigned 발급 API 추가
    @PostMapping("/download-pdf")
    public ResponseEntity<ReportPdfDownloadUrlResponseDto> getReportPdfDownloadUrl(
            @RequestBody ReportPdfDownloadUrlRequestDto requestDto,
            @RequestHeader("accessToken") String accessToken) {

        Long userId = 1L;

        Report report = reportRepository.findById(requestDto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("리포트가 존재하지 않습니다."));

        String s3Key = report.getPdfKey();

        if (s3Key == null) {
            throw new IllegalStateException("PDF가 아직 생성되지 않았습니다.");
        }

        // userId 검증 없이 직접 presigned 생성
        String downloadUrl = s3UploadService.generateDownloadPresignedUrl(s3Key);

        return ResponseEntity.ok(new ReportPdfDownloadUrlResponseDto(downloadUrl, 300));
    }
}