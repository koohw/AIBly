package com.ssafy.backend.video.controller;

import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;
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

    @PostMapping("/upload-notify") //S3에 presigned 방식으로 업로드된 영상 정보를 서버에 전달하여 DB에 저장하고 분석
    public ResponseEntity<UploadNotifyResponseDto> autoUpload(
            @RequestBody UploadNotifyRequestDto request
            // 추후 @AuthenticationPrincipal UserDetails user 등으로 교체
    ) {
        // 임시 userId: 로그인 연동 전 테스트용
        Long userId = 1L;

        UploadNotifyResponseDto response = videoService.registerUploadedVideo(request, userId);
        return ResponseEntity.ok(response);
    }
}
