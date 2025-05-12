package com.ssafy.backend.video.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;

import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;
import com.ssafy.backend.video.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController extends BaseController {

    private final VideoService videoService;

    //갤러리에서 자동으로 업로드된 영상 등록
    @PostMapping("/upload-notify/auto")
    public ResponseEntity<UploadNotifyResponseDto> autoUpload(
            @RequestBody UploadNotifyRequestDto request,
            HttpServletRequest httpRequest
    ) {
        //Long userId = 1L; // 임시 사용자
        Long userId = getCurrentUserId(httpRequest); // ✔ BaseController 메서드 사용
        UploadType uploadType = UploadType.AUTO;

        UploadNotifyResponseDto response = videoService.registerUploadedVideo(request, userId, uploadType);
        return ResponseEntity.ok(response);
    }

    //사용자가 직접 업로드
    @PostMapping("/upload-notify/manual")
    public ResponseEntity<UploadNotifyResponseDto> manualUpload(
            @RequestBody UploadNotifyRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        UploadType uploadType = UploadType.MANUAL;

        UploadNotifyResponseDto response = videoService.registerUploadedVideo(request, userId, uploadType);
        return ResponseEntity.ok(response);
    }
}
