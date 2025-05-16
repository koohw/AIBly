package com.ssafy.backend.report.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
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
public class PdfServiceImpl implements PdfService {

    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    private static final String TEMPLATE_PATH = "templates/report-pdf.html";
    private static final String S3_DIR = "report-pdf/";

    @Transactional
    @Override
    public String generateAndUploadPdf(Long reportId) {
        // 1. Report 조회
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        // 2. 이미 PDF 생성 여부 확인 (중복 방지)
        if (report.getPdfKey() != null) {
            throw new CustomException(ErrorCode.PDF_ALREADY_EXIST);
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
            throw new CustomException(ErrorCode.PDF_GENERATE_FAIL);
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

            // 폰트 파일과 베이스 URL 설정
            File fontFile = null;
            String baseUrl = null;
            
            // 1. 먼저 EC2 배포 환경 폰트 경로 확인
            File ec2FontFile = new File("/home/ubuntu/font/NotoSansKR-Regular.ttf");
            if (ec2FontFile.exists()) {
                fontFile = ec2FontFile;
                baseUrl = ec2FontFile.getParentFile().toURI().toString();
                System.out.println("EC2 환경 폰트 파일 사용: " + fontFile.getAbsolutePath());
            } else {
                // 2. 로컬 환경에서 폰트 파일 확인
                try {
                    fontFile = new ClassPathResource("fonts/NotoSansKR-Regular.ttf").getFile();
                    baseUrl = fontFile.getParentFile().toURI().toString();
                    System.out.println("로컬 환경 폰트 파일 사용: " + fontFile.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("로컬 환경 폰트 파일 로드 실패: " + e.getMessage());
                    throw new RuntimeException("폰트 파일을 찾을 수 없습니다. EC2와 로컬 환경 모두 실패했습니다.");
                }
            }

            // 3. HTML 컨텐츠에 폰트 디렉토리 기준 베이스 URL 설정
            builder.withHtmlContent(processedHtml, baseUrl);

            // 4. 폰트 설정 및 임베딩
            builder.useFont(fontFile, "Noto Sans KR", 400, PdfRendererBuilder.FontStyle.NORMAL, true);

            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF 생성 실패 (한글 깨짐 가능): " + e.getMessage(), e);
        }
    }

}
