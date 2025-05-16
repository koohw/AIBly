package com.ssafy.backend.report.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    private static final String TEMPLATE_PATH = "templates/report-pdf.html";
    private static final String FONT_PATH = "fonts/NotoSansKR-Regular.ttf";
    private static final String S3_DIR = "report-pdf/";

    @Transactional
    @Override
    public String generateAndUploadPdf(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getPdfKey() != null) {
            throw new CustomException(ErrorCode.PDF_ALREADY_EXIST);
        }

        String s3Key = S3_DIR + UUID.randomUUID() + ".pdf";

        try {
            String processedHtml = processTemplate(buildVariables(report));
            byte[] pdfBytes = generatePdfFromHtml(processedHtml);
            s3UploadService.uploadPdf(pdfBytes, s3Key, "application/pdf");

            report.setPdfKey(s3Key);
            reportRepository.saveAndFlush(report);

            return s3Key;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ErrorCode.PDF_GENERATE_FAIL);
        }
    }

    private Map<String, String> buildVariables(Report report) {
        Map<String, String> variables = new HashMap<>();
        variables.put("title", escape(report.getTitle()));
        variables.put("accidentType", escape(report.getAccidentType()));
        variables.put("carA", escape(report.getCarA()));
        variables.put("carB", escape(report.getCarB()));
        variables.put("mainEvidence", escape(report.getMainEvidence()));
        variables.put("laws", escape(report.getLaws()));
        variables.put("createdAt", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return variables;
    }

    private String escape(String value) {
        return StringEscapeUtils.escapeHtml4(value != null ? value : "");
    }

    private String processTemplate(Map<String, String> variables) {
        try {
            String template = new String(
                    new ClassPathResource(TEMPLATE_PATH).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                template = template.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        } catch (Exception e) {
            throw new RuntimeException("HTML 템플릿 처리 실패", e);
        }
    }

    private byte[] generatePdfFromHtml(String processedHtml) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 폰트 파일을 바이트 배열로 로드
            byte[] fontBytes;
            try (InputStream is = new ClassPathResource(FONT_PATH).getInputStream()) {
                fontBytes = is.readAllBytes();
            }

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> new java.io.ByteArrayInputStream(fontBytes), "Noto Sans KR", 400, PdfRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(processedHtml, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF 생성 실패 (한글 깨짐 가능)", e);
        }
    }
}
