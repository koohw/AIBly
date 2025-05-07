package com.ssafy.backend.s3.controller;

import com.ssafy.backend.s3.model.dto.PresignedUrlRequestDto;
import com.ssafy.backend.s3.model.dto.PresignedUrlResponseDto;
import com.ssafy.backend.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3UploadService s3UploadService;

    // presigned URL을 생성하여 프론트에 전달
    @PostMapping("/presigned")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrl(@RequestBody PresignedUrlRequestDto request) {
        String s3Key = s3UploadService.generateS3Key(request.getFileName());
        String url = s3UploadService.generatePresignedUrl(s3Key, request.getContentType());

        return ResponseEntity.ok(new PresignedUrlResponseDto(url, s3Key));
    }
}