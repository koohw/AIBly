package com.ssafy.backend.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.service.AiAnalysisService;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.fcm.service.FcmService;
import com.ssafy.backend.user.service.UserService;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class AiInternalController {

    private final AiAnalysisService aiAnalysisService;
    private final UserService userService;
    private final FcmService fcmService;
    private final VideoService videoService;

    @PostMapping("/ai-callback")
    public ResponseEntity<Void> handleAICallback(@RequestBody JsonNode json,
                                                 @RequestParam("videoId") Long videoId) {
        log.info("AI Callback 수신: videoId={}", videoId);

        Long reportId = aiAnalysisService.handleAiCallback(json, videoId);
        log.info("Report 생성 완료: reportId={}, videoId={}", reportId, videoId);

        // 업로드 타입 확인
        UploadType uploadType = videoService.getUploadType(videoId);
        log.info("업로드 타입 확인: videoId={}, uploadType={}", videoId, uploadType);

        if (uploadType == UploadType.AUTO) {
            String fcmToken = userService.getUserFcmTokenByVideoId(videoId);
            if (fcmToken == null || fcmToken.isBlank()) {
                log.info("FCM 토큰 없음 - FCM 발송 없음: videoId={}", videoId);
            } else {
                try {
                    fcmService.sendFCM(fcmToken, reportId);
                    log.info("FCM 발송 성공: videoId={}, reportId={}", videoId, reportId);
                } catch (Exception e) {
                    log.error("FCM 발송 실패: videoId={}, error={}", videoId, e.getMessage(), e);
                }
            }
        } else {
            log.info("수동 업로드 - FCM 발송 없음: videoId={}", videoId);
        }

        return ResponseEntity.ok().build();
    }

}
