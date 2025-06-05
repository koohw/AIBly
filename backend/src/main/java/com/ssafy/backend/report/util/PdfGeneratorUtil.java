package com.ssafy.backend.report.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder; //openhtmltopdf->HTML을 PDF로 렌더링하는 라이브러리
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PdfGeneratorUtil {

    public byte[] generatePdfFromTemplate(String templatePath, Map<String, String> variables) {
        try (InputStream inputStream = new ClassPathResource(templatePath).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            StringBuilder htmlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                htmlBuilder.append(line).append("\n");
            }

            String html = htmlBuilder.toString();
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                html = html.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(html, null);
                builder.toStream(outputStream);
                builder.run();
                return outputStream.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }
}

//PDF 생성을 위한 유틸 클래스
//HTML 템플릿을 읽고, ${title} 같은 값을 Java에서 치환한 뒤, PDF 바이트로 변환