package com.ssafy.backend.report.controller;

import com.ssafy.backend.report.dto.AIAnalysisResultRequestDto;
import com.ssafy.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/ai-callback")
    public ResponseEntity<Void> handleAICallback(
            @RequestBody AIAnalysisResultRequestDto request,
            @RequestParam("videoId") Long videoId
    ) {
        reportService.generateReport(request, videoId);
        return ResponseEntity.ok().build();
    }
}
