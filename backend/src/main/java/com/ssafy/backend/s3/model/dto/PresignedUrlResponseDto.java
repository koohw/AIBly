package com.ssafy.backend.s3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponseDto {
    private String presignedUrl;  // PUT으로 업로드할 presigned URL. 프론트가 직접 put으로 s3에 업로드함.
    private String s3Key;  // 서버가 저장할 고유 파일 키
}

//백엔드가 presigned URL 생성 후 클라이언트에게 주는 응답