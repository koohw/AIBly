package com.ssafy.backend.report.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    private static final String TEMPLATE_PATH = "templates/report-pdf.html";
    private static final String S3_DIR = "report-pdf/";

    @Transactional
    public String generateAndUploadPdf(Long reportId) {
        // 1. 리포트 조회
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        // 2. 템플릿 치환용 변수 구성
        Map<String, String> variables = new HashMap<>();
        variables.put("title", report.getTitle());
        variables.put("accidentType", report.getAccidentType());
        variables.put("carA", report.getCarA());
        variables.put("carB", report.getCarB());
        variables.put("mainEvidence", report.getMainEvidence());
        variables.put("laws", report.getLaws());
        variables.put("createdAt", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // 3. 템플릿 처리 → HTML 생성
        String processedHtml = processTemplate(variables);

        // 4. HTML → PDF 생성 (한글 깨짐 방지 포함)
        byte[] pdfBytes = generatePdfFromHtml(processedHtml);

        // 5. S3 업로드
        String s3Key = S3_DIR + UUID.randomUUID() + ".pdf";
        s3UploadService.uploadPdf(pdfBytes, s3Key, "application/pdf");

        // 6. 리포트에 S3 Key 저장
        report.setPdfKey(s3Key);

        return s3Key;
    }

    /**
     * 템플릿 HTML 처리 (간단한 치환)
     */
    private String processTemplate(Map<String, String> variables) {
        try {
            String template = new String(new ClassPathResource(TEMPLATE_PATH).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                template = template.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        } catch (Exception e) {
            throw new RuntimeException("템플릿 처리 실패", e);
        }
    }

    /**
     * 한글 깨짐 방지를 포함한 PDF 생성
     */
    private byte[] generatePdfFromHtml(String processedHtml) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(processedHtml, null);

            String fontPath = new ClassPathResource("fonts/NotoSansCJKkr-Regular.otf").getFile().getAbsolutePath();
            builder.useFont(new File(fontPath), "Noto Sans CJK KR");

            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }
}
