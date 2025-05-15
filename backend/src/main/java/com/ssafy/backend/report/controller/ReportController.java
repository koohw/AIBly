package com.ssafy.backend.report.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.dto.response.ReportPdfDownloadResponseDto;
import com.ssafy.backend.report.service.PdfService;
import com.ssafy.backend.s3.service.S3UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private final PdfService pdfService;
    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    //pdf로 변환하고 presiged url 반환해서 다운로드 할 수 있게 함
    @PostMapping("/{reportId}/pdf")
    public ResponseEntity<ReportPdfDownloadResponseDto> saveAndDownloadPdf(@PathVariable Long reportId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        // 1. PDF 생성 & S3 업로드 → s3Key 생성
        String s3Key = pdfService.generateAndUploadPdf(reportId);

        // 2. Report Entity 업데이트 (pdfKey 저장)
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트가 존재하지 않습니다."));
        report.setPdfKey(s3Key);

        // 3. presigned URL 발급
        String downloadUrl = s3UploadService.generateDownloadPresignedUrl(s3Key);

        // 4. URL 반환 (다운로드 가능)
        return ResponseEntity.ok(new ReportPdfDownloadResponseDto(downloadUrl, 300));
    }
}