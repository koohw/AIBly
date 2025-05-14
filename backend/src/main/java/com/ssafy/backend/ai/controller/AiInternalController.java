package com.ssafy.backend.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.backend.ai.service.AiAnalysisService;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.fcm.service.FcmService;
import com.ssafy.backend.user.service.UserService;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


        Long reportId = aiAnalysisService.handleAiCallback(json, videoId);

        // 업로드 타입 확인
        UploadType uploadType = videoService.getUploadType(videoId);
        if (uploadType == UploadType.AUTO) {
            String fcmToken = userService.getUserFcmTokenByVideoId(videoId);
            fcmService.sendFCM(fcmToken, reportId);
        }

        // 수동 업로드는 FCM 전송 없이 Report 상태만 업데이트 완료 처리
        return ResponseEntity.ok().build();
    }
}
