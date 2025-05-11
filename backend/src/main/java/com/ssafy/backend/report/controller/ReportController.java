package com.ssafy.backend.report.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class ReportController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/ai-callback")
    public ResponseEntity<Void> handleAICallback(
            @RequestBody JsonNode json,
            @RequestParam("videoId") Long videoId
    ) {
        aiAnalysisService.handleAiCallback(json, videoId);
        return ResponseEntity.ok().build();
    }
}
