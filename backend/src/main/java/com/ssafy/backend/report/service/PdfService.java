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
        // 1. Report 조회
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        // 2. 이미 PDF 생성 여부 확인 (중복 방지)
        if (report.getPdfKey() != null) {
            throw new IllegalStateException("이미 PDF가 생성된 리포트입니다.");
        }

        // 3. PDF Key 생성 (UUID 사용)
        String s3Key = S3_DIR + UUID.randomUUID() + ".pdf";

        try {
            // 4. 템플릿 치환 → HTML 생성
            String processedHtml = processTemplate(buildVariables(report));

            // 5. HTML → PDF 생성 (한글 깨짐 방지 포함)
            byte[] pdfBytes = generatePdfFromHtml(processedHtml);

            // 6. S3 업로드
            s3UploadService.uploadPdf(pdfBytes, s3Key, "application/pdf");

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 또는 S3 업로드 실패: " + e.getMessage(), e);
        }

        // 7. S3 업로드 성공 후에만 DB update (안전하게)
        report.setPdfKey(s3Key);

        reportRepository.saveAndFlush(report);

        return s3Key;
    }

    /**
     * Report에서 템플릿 치환 변수 추출
     */
    private Map<String, String> buildVariables(Report report) {
        Map<String, String> variables = new HashMap<>();
        variables.put("title", report.getTitle());
        variables.put("accidentType", report.getAccidentType());
        variables.put("carA", report.getCarA());
        variables.put("carB", report.getCarB());
        variables.put("mainEvidence", report.getMainEvidence());
        variables.put("laws", report.getLaws());
        variables.put("createdAt", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return variables;
    }

    /**
     * HTML 템플릿 치환
     */
    private String processTemplate(Map<String, String> variables) {
        try {
            String template = new String(new ClassPathResource(TEMPLATE_PATH).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                template = template.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        } catch (Exception e) {
            throw new RuntimeException("HTML 템플릿 처리 실패", e);
        }
    }

    /**
     * HTML → PDF 변환 (폰트 임베딩 강제 → 한글 깨짐 방지)
     */
    private byte[] generatePdfFromHtml(String processedHtml) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(processedHtml, null);

            // ✅ 서버 리소스에서 한글 폰트 임베딩
            String fontPath = new ClassPathResource("fonts/NotoSansKR-Regular.ttf").getFile().getAbsolutePath();
            builder.useFont(new File(fontPath), "Noto Sans KR");

            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF 생성 실패 (한글 깨짐 가능)", e);
        }
    }
}
