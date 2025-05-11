package com.ssafy.backend.s3.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PresignedUrlRequestDto {
    private String FileName; // 사용자가 업로드한 원본 파일명
    private String contentType;      // MIME 타입 (video/mp4)
}

//클라이언트가 presigned URL 생성을 요청할 때 보내는 데이터