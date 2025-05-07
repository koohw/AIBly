package com.ssafy.backend.video.controller;

import com.ssafy.backend.video.dto.response.VideoUploadResponse;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @PostMapping("/auto-upload")
    public ResponseEntity<VideoUploadResponse> autoUpload(@RequestParam("file") MultipartFile file){
        return ResponseEntity.ok(videoService.autoUpload(file));
    }
}
