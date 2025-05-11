package com.ssafy.backend.report.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.service.AiAnalysisService;
import com.ssafy.backend.report.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfService pdfService;

    @PostMapping("/save-pdf")
    public ResponseEntity<Map<String, String>> savePdf(@RequestParam Long reportId) {
        String s3Key = pdfService.generateAndUploadPdf(reportId);
        return ResponseEntity.ok(Map.of(
                "message", "PDF 저장 완료",
                "s3Key", s3Key
        ));
    }
}

