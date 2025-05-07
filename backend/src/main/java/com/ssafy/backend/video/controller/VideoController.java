package com.ssafy.backend.video.controller;

import com.ssafy.backend.video.dto.AutoUploadRequestDto;
import com.ssafy.backend.video.dto.AutoUploadResponseDto;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/auto-upload")
    public ResponseEntity<AutoUploadResponseDto> autoUpload(
            @RequestBody AutoUploadRequestDto request
            // 추후 @AuthenticationPrincipal UserDetails user 등으로 교체
    ) {
        // 임시 userId: 로그인 연동 전 테스트용
        Long userId = 1L;

        AutoUploadResponseDto response = videoService.saveAutoUploadedVideo(request, userId);
        return ResponseEntity.ok(response);
    }
}
