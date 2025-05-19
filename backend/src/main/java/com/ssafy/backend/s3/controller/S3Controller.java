package com.ssafy.backend.s3.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.domain.file.FileType;
import com.ssafy.backend.domain.file.S3File;
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

    //같은 영상이라 s3key가 같다면 presigedUrl만 새로 발급 받음
    @PostMapping("/presigned")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrl(
            @RequestBody PresignedUrlRequestDto request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);

        String s3Key = s3UploadService.getOrCreateS3Key(
                request.getFileName(), request.getContentType(), userId);

        String presignedUrl = s3UploadService.generatePresignedUrl(s3Key, request.getContentType());

        return ResponseEntity.ok(new PresignedUrlResponseDto(presignedUrl, s3Key));
    }


    // presigned URL을 생성하여 프론트에 전달, 계속 같은 영상이더라도 S3 KEY와 URL 함께 새로 발급
    //Presigned URL 요청과 동시에 DB에 S3File 기록
    @PostMapping("/new-url-key")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrlKey(
            @RequestBody PresignedUrlRequestDto request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);

        if (s3UploadService.isDuplicateFile(request.getFileName(), request.getContentType(), userId)) {
            throw new RuntimeException("이미 업로드된 파일입니다.");
        }

        String s3Key = s3UploadService.generateS3Key(request.getFileName());
        String presignedUrl = s3UploadService.generatePresignedUrl(s3Key, request.getContentType());

        S3File file = S3File.builder()
                .s3Key(s3Key)
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .userId(userId)
                .fileType(FileType.VIDEO)
                .build();

        s3UploadService.saveS3File(file);

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

}