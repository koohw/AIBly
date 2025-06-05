package com.ssafy.backend.video.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;

import com.ssafy.backend.video.dto.response.MyVideoResponseDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;
import com.ssafy.backend.video.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        UploadNotifyResponseDto response = videoService.registerUploadedVideo(request, userId, UploadType.AUTO); //업로드 타입에 따라 자동은 fcm발송, 수동은 앱에서 polling처리

        return ResponseEntity.ok(response);
    }

    //사용자가 직접 업로드
    @PostMapping("/upload-notify/manual")
    public ResponseEntity<UploadNotifyResponseDto> manualUpload(
            @RequestBody UploadNotifyRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        UploadNotifyResponseDto response = videoService.registerUploadedVideo(request, userId, UploadType.MANUAL);

        return ResponseEntity.ok(response);
    }

    //영상 목록
    @GetMapping("/list/content")
    public ResponseEntity<List<MyVideoResponseDto>> getMyVideos(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return ResponseEntity.ok(videoService.getMyVideos(userId));
    }

    //상세 영상
    @GetMapping("/detail-content/{videoId}")
    public ResponseEntity<MyVideoResponseDto> getMyVideoDetail(@PathVariable Long videoId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return ResponseEntity.ok(videoService.getMyVideoDetail(userId, videoId));
    }

    // 내 영상함에서 영상 삭제 (보고서 + PDF 포함 전부 삭제)
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteMyVideo(@PathVariable Long videoId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        videoService.deleteVideoAndReport(userId, videoId);
        return ResponseEntity.noContent().build();
    }

}
