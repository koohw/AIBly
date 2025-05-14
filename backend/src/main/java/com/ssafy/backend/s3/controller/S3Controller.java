package com.ssafy.backend.s3.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.s3.dto.request.PresignedUrlRequestDto;
import com.ssafy.backend.s3.dto.response.PresignedUrlResponseDto;
import com.ssafy.backend.s3.dto.request.PresignedDownloadRequestDto;
import com.ssafy.backend.s3.service.S3UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller extends BaseController {

//    private final JWTUtil jwtUtil;

    private final S3UploadService s3UploadService;

    // presigned URL을 생성하여 프론트에 전달
    @PostMapping("/presigned")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrl(@RequestBody PresignedUrlRequestDto request) {
        String s3Key = s3UploadService.generateS3Key(request.getFileName());
        String presignedUrl = s3UploadService.generatePresignedUrl(s3Key, request.getContentType());

        return ResponseEntity.ok(new PresignedUrlResponseDto(presignedUrl, s3Key));
    }

    // presigned URL 을 생성 (다운로드 용도, FE용도...)
    // jwt 관련 기능이 없어서 임시로 userId를 1로 진행함.
    @PostMapping("/downloadURL")
    public ResponseEntity<?> getDownloadURL(
            HttpServletRequest request,
            @RequestBody PresignedDownloadRequestDto dto
    ) {

        Long userId = getCurrentUserId(request);
        String url = s3UploadService.getDownloadURL(userId, dto.getS3Key());
        return ResponseEntity.ok(url);
    }

    // S3에 업로드된 파일을 삭제
    @PostMapping("/deleteFile")
    public ResponseEntity<?> deleteFile(
            HttpServletRequest request,
            //@RequestHeader String accessToken,
            @RequestBody PresignedDownloadRequestDto dto
    ){

        Long userId = getCurrentUserId(request);
        s3UploadService.deleteS3File(userId, dto.getS3Key());
        return ResponseEntity.noContent().build();
    }
}