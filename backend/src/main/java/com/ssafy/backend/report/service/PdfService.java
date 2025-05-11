package com.ssafy.backend.report.service;

import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.util.PdfGeneratorUtil;
import com.ssafy.backend.s3.service.S3UploadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final ReportRepository reportRepository;
    private final PdfGeneratorUtil pdfGeneratorUtil;
    private final S3UploadService s3UploadService;

    private static final String TEMPLATE_PATH = "templates/report-pdf.html";
    private static final String S3_DIR = "report-pdf/";

    @Transactional
    public String generateAndUploadPdf(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        // 템플릿 치환용 변수 구성
        Map<String, String> variables = new HashMap<>();
        variables.put("title", report.getTitle());
        variables.put("accidentType", report.getAccidentType());
        variables.put("carA", report.getCarA());
        variables.put("carB", report.getCarB());
        variables.put("mainEvidence", report.getMainEvidence());
        variables.put("laws", report.getLaws());
        variables.put("createdAt", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // PDF 생성
        byte[] pdfBytes = pdfGeneratorUtil.generatePdfFromTemplate(TEMPLATE_PATH, variables);

        // S3 업로드
        String s3Key = S3_DIR + UUID.randomUUID() + ".pdf";
        s3UploadService.uploadPdf(pdfBytes, s3Key, "application/pdf");

        // Report에 S3 Key 저장
        report.setPdfKey(s3Key);

        return s3Key;
    }
}
