package com.ssafy.backend.ai.controller;

import com.ssafy.backend.ai.service.AiService;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class AiInternalController {

    private final AiService aiService;
    private final VideoFileRepository videoFileRepository;

    @PostMapping("/analyze/{videoId}")
    public ResponseEntity<Void> analyzeVideo(@PathVariable Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("VideoFile not found: " + videoId));

        aiService.requestAndHandleAnalysis(videoFile);

        return ResponseEntity.ok().build();
    }
}